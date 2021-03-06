/*
 * FAE, Feinno App Engine
 *  
 * Create by gaolei 2010-12-29
 * 
 * Copyright (c) 2010 北京新媒传信科技有限公司
 */
package com.feinno.configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feinno.configuration.spi.ConfigUpdater;
import com.feinno.ha.ServiceSettings;
import com.feinno.serialization.protobuf.ProtoEntity;
import com.feinno.util.SearchIndex;
import com.feinno.util.ServiceEnviornment;

/**
 * 配置管理器
 * 
 * @author 高磊 gaolei@feinno.com
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ConfigurationManager {
	private static Configurator configurator;
	private static Object syncRoot = new Object();
	private static List<ConfigUpdater<?>> updaters;
	private static SearchIndex<ConfigUpdater<?>> updaterIndex;
	private static Logger LOGGER = LoggerFactory.getLogger(ConfigurationManager.class);

	static {
		updaters = new ArrayList<ConfigUpdater<?>>();
		String[] params = new String[] { "path", "type", "params" };
		try {
			updaterIndex = new SearchIndex(ConfigUpdater.class, updaters, params);
		} catch (Exception e) {
			LOGGER.error(e.toString());
		}
	}

	/**
	 * 
	 * 设置配置加载器, 可能存在本地配置加载或全局配置加载两种情况
	 * 
	 * @param loader
	 */
	public static void setConfigurator(Configurator configurator) {
		ConfigurationManager.configurator = configurator;
	}

	/**
	 * 
	 * 获取一个配置表
	 * <p>
	 * 1. 当属于LOCAL模式时, 配置表将从配置根目录下的configdb.properties中读取<br>
	 * 2. 当处于HA模式时, 配置表将会通过HACenter读取<br>
	 * </p>
	 * 
	 * @see ConfigTableItem
	 * @see ConfigTableKey
	 * @param <K>
	 * @param <V>
	 * @param keyType
	 *            配置表主键类型, 可以是基础类型, 或者组合类型 , 当为组合类型时必须从ConfigTableKey中派生
	 * @param valueType
	 *            配置表值类型, 必须从ConfigTableItem类派生
	 * @param path
	 *            表名
	 * @param updateCallback
	 *            更新的callback
	 * @throws 当配置表不存在或字段不匹配时
	 *             , 将抛出ConfigurationException
	 */
	public static <K, V extends ConfigTableItem> ConfigTable<K, V> loadTable(final Class<K> keyType,
			final Class<V> valueType, final String path, final ConfigUpdateAction<ConfigTable<K, V>> updateCallback)
			throws ConfigurationException {
		return loadTable(configurator, keyType, valueType, path, updateCallback);
	}

	/**
	 * 
	 * 获取一个配置表
	 * <p>
	 * 1. 当属于LOCAL模式时, 配置表将从配置根目录下的configdb.properties中读取<br>
	 * 2. 当处于HA模式时, 配置表将会通过HACenter读取<br>
	 * </p>
	 * 
	 * @see ConfigTableItem
	 * @see ConfigTableKey
	 * @param <K>
	 * @param <V>
	 * @param configurator
	 *            自定义的配置加载器
	 * @param keyType
	 *            配置表主键类型, 可以是基础类型, 或者组合类型 , 当为组合类型时必须从ConfigTableKey中派生
	 * @param valueType
	 *            配置表值类型, 必须从ConfigTableItem类派生
	 * @param path
	 *            表名
	 * @param updateCallback
	 *            更新的callback
	 * @throws 当配置表不存在或字段不匹配时
	 *             , 将抛出ConfigurationException
	 */
	public static <K, V extends ConfigTableItem> ConfigTable<K, V> loadTable(final Configurator configurator,
			final Class<K> keyType, final Class<V> valueType, final String path,
			final ConfigUpdateAction<ConfigTable<K, V>> updateCallback) throws ConfigurationException {
		try {
			// 订阅用于配置变动时的更新
			ConfigUpdater<ConfigTable<K, V>> updater = new ConfigUpdater<ConfigTable<K, V>>(path, ConfigType.TABLE, "") {
				@Override
				public void update() throws Exception {
					configurator.loadConfigTable(keyType, valueType, path, updateCallback);
				}
			};
			synchronized (syncRoot) {
				updaters.add(updater);
				updaterIndex.build(updaters);
			}
			return configurator.loadConfigTable(keyType, valueType, path, updateCallback);
		} catch (Exception e) {
			throw new ConfigurationException("loadTable failed: " + path, e);
		}
	}

	/**
	 * 
	 * 获取一个配置的文本 服务器当中的Config结构为一个树状结构 \ \resource\ \database \file \application
	 * 在本机中,都保存在本进程下属的conf文件中
	 * 
	 * @param path
	 * @param args
	 * @param udpateCallback
	 * @return 当不存在该项配置时返回空字符串,空输入流,或者空Properties
	 * @throws ConfigurationException
	 */
	public static String loadText(final String path, final ConfigParams params,
			final ConfigUpdateAction<String> updateCallback) throws ConfigurationException {
		return loadText(configurator, path, params, updateCallback);
	}

	/**
	 * 
	 * 获取一个配置的文本 服务器当中的Config结构为一个树状结构 \ \resource\ \database \file \application
	 * 在本机中,都保存在本进程下属的conf文件中
	 * 
	 * @param configurator
	 *            自定义的配置加载器
	 * @param path
	 * @param args
	 * @param udpateCallback
	 * @return 当不存在该项配置时返回空字符串,空输入流,或者空Properties
	 * @throws ConfigurationException
	 */
	public static String loadText(final Configurator configurator, final String path, final ConfigParams params,
			final ConfigUpdateAction<String> updateCallback) throws ConfigurationException {
		try {

			// 首先包装进当前的机器与服务名称
			final ConfigParams paramsWrapper = configParamDecoration(params);

			// 订阅用于配置变动时的更新
			ConfigUpdater<String> updater = new ConfigUpdater<String>(path, ConfigType.TEXT,
					paramsWrapper != null ? paramsWrapper.toString() : "") {
				@Override
				public void update() throws Exception {
					configurator.loadConfigText(path, paramsWrapper, updateCallback);
				}
			};
			synchronized (syncRoot) {
				updaters.add(updater);
				updaterIndex.build(updaters);
			}
			return configurator.loadConfigText(path, paramsWrapper, updateCallback);
		} catch (Exception e) {
			throw new ConfigurationException("loadText error", e);
		}
	}

	/**
	 * 
	 * 获取一个配置的字节类型
	 * 
	 * @param path
	 * @param args
	 * @param udpateCallback
	 * @return 当不存在该项配置时返回空字符串,空输入流,或者空Properties
	 * @throws ConfigurationException
	 */
	public static <T extends ProtoEntity> T loadConfig(final ConfigType type, final String path,
			final ConfigParams params, final ConfigUpdateAction<T> updateCallback) throws ConfigurationException {
		return loadConfig(configurator, type, path, params, updateCallback);
	}

	/**
	 * 
	 * 获取一个配置的字节类型
	 * 
	 * @param configurator
	 *            自定义的配置加载器
	 * @param path
	 * @param args
	 * @param udpateCallback
	 * @return 当不存在该项配置时返回空字符串,空输入流,或者空Properties
	 * @throws ConfigurationException
	 */
	public static <T extends ProtoEntity> T loadConfig(final Configurator configurator, final ConfigType type,
			final String path, final ConfigParams params, final ConfigUpdateAction<T> updateCallback)
			throws ConfigurationException {
		try {

			// 首先包装进当前的机器与服务名称
			final ConfigParams paramsWrapper = configParamDecoration(params);

			// 订阅用于配置变动时的更新
			ConfigUpdater<String> updater = new ConfigUpdater<String>(path, type,
					paramsWrapper != null ? paramsWrapper.toString() : "") {
				@Override
				public void update() throws Exception {
					configurator.loadConfig(type, path, paramsWrapper, updateCallback);
				}
			};
			synchronized (syncRoot) {
				updaters.add(updater);
				updaterIndex.build(updaters);
			}
			return configurator.loadConfig(type, path, paramsWrapper, updateCallback);
		} catch (Exception e) {
			throw new ConfigurationException("loadConfig error", e);
		}
	}

	/**
	 * 
	 * 获取一个配置的输入流, 实质上是在获取一个文本之上的封装
	 * 
	 * @param path
	 * @param args
	 * @param updateCallback
	 * @return 当不存在该项配置时返回空字符串,空输入流,或者空Properties
	 * @throws ConfigurationException
	 */
	public static InputStream loadTextStream(final String path, final ConfigParams params,
			final ConfigUpdateAction<InputStream> updateCallback) throws ConfigurationException {
		try {

			// 首先包装进当前的机器与服务名称
			final ConfigParams paramsWrapper = configParamDecoration(params);

			// 订阅用于配置变动时的更新
			ConfigUpdater<String> updater = new ConfigUpdater<String>(path, ConfigType.TEXT,
					paramsWrapper != null ? paramsWrapper.toString() : "") {
				@Override
				public void update() throws Exception {
					configurator.loadConfigText(path, paramsWrapper, new ConfigUpdateAction<String>() {
						@Override
						public void run(String text) throws Exception {
							if (updateCallback != null) {
								updateCallback.run(convertToStream(text));
							}
						}
					});
				}
			};
			synchronized (syncRoot) {
				updaters.add(updater);
				updaterIndex.build(updaters);
			}

			// 执行本次配置获取
			return convertToStream(configurator.loadConfigText(path, paramsWrapper, new ConfigUpdateAction<String>() {
				@Override
				public void run(String text) throws Exception {
					if (updateCallback != null) {
						updateCallback.run(convertToStream(text));
					}
				}
			}));
		} catch (Exception e) {
			throw new ConfigurationException("loadTextStream error", e);
		}
	}

	/**
	 * 
	 * 获取一个配置的Properties, 实质上是在获取一个文本之上的封装
	 * 
	 * @param path
	 * @param args
	 * @param updateCallback
	 * @return 当不存在该项配置时返回空字符串,空输入流,或者空Properties
	 * @throws IOException
	 * @throws ConfigurationException
	 */
	public static Properties loadProperties(final String path, final ConfigParams params,
			final ConfigUpdateAction<Properties> updateCallback) throws ConfigurationException {
		try {
			// 首先包装进当前的机器与服务名称
			final ConfigParams paramsWrapper = configParamDecoration(params);

			// 订阅配置自动更新
			ConfigUpdater<String> updater = new ConfigUpdater<String>(path, ConfigType.TEXT,
					paramsWrapper != null ? paramsWrapper.toString() : "") {
				@Override
				public void update() throws Exception {
					configurator.loadConfigText(path, paramsWrapper, new ConfigUpdateAction<String>() {
						@Override
						public void run(String text) throws Exception {
							if (updateCallback != null) {
								updateCallback.run(convertToProperies(text));
							}
						}
					});
				}
			};
			synchronized (syncRoot) {
				updaters.add(updater);
				updaterIndex.build(updaters);
			}
			// 本次调用结果返回

			return convertToProperies(configurator.loadConfigText(path, paramsWrapper,
					new ConfigUpdateAction<String>() {
						@Override
						public void run(String text) throws Exception {
							if (updateCallback != null) {
								updateCallback.run(convertToProperies(text));
							}
						}
					}));
		} catch (Exception e) {
			throw new ConfigurationException("loadProperties failed:" + path, e);
		}
	}

	/**
	 * 
	 * 订阅配置更新
	 * 
	 * @param type
	 * @param path
	 */
	public static void subscribeConfigUpdate(ConfigType type, String path, ConfigParams params) {
		configurator.subscribeConfig(type, path, params);
	}

	/**
	 * 
	 * Push方式更新配置
	 * 
	 * @param path
	 * @param type
	 */
	public static void updateConfig(ConfigType type, String path, String params) throws ConfigurationException {
		try {
			List<ConfigUpdater<?>> list = updaterIndex.find(path, type, params);
			if (list != null && list.size() > 0)
				for (ConfigUpdater updater : list)
					updater.update();
		} catch (Exception e) {
			String msg = String.format("updateConfig(%s, %s) FAILED!", path, type.toString());
			LOGGER.error(msg, e);
			throw new ConfigurationException(msg, e);
		}
	}

	/**
	 * 将一个UTF-8的字符串转换为一个InputBuffer缓冲区
	 * 
	 * @param text
	 * @return
	 */
	public static InputStream convertToStream(String text) {
		if (text == null)
			return null;
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		return stream;
	}

	/**
	 * 将一个UTF-8的字符串转换为一个Properties对象
	 * 
	 * @param text
	 * @return
	 * @throws IOException
	 */
	public static Properties convertToProperies(String text) throws IOException {
		if (text == null)
			return null;
		Properties prop = new Properties();
		StringReader reader = new StringReader(text);
		prop.load(reader);
		return prop;
	}

	private static ConfigParams configParamDecoration(ConfigParams params) {
		if (params == null) {
			params = new ConfigParams();
		}
		params.put("service", ServiceSettings.INSTANCE.getServiceName());
		params.put("computer", ServiceEnviornment.getComputerName());
		return params;
	}
}
