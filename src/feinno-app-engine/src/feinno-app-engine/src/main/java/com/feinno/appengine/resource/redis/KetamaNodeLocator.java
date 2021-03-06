package com.feinno.appengine.resource.redis;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.TreeMap;

public final class KetamaNodeLocator<T>
{
	public static final Integer VIRTUAL_NODE_COUNT = 160;	
	
	// 添加日志记录
	private TreeMap<Long, T> ketamaNodes;
	private int numReps = 160;

	public KetamaNodeLocator(List<T> nodes, int nodeCopies)
	{
		ketamaNodes = new TreeMap<Long, T>();

		numReps = nodeCopies;

		for (T node : nodes) {
			for (int i = 0; i < numReps / 4; i++) {
				byte[] digest = computeMd5(node.toString() + i);
				for (int h = 0; h < 4; h++) {
					long m = hash(digest, h);
					ketamaNodes.put(m, node);
				}
			}
		}
	}
	
	public KetamaNodeLocator(T[] nodes, int nodeCopies)
	{
		ketamaNodes = new TreeMap<Long, T>();

		numReps = nodeCopies;

		for (T node : nodes) {
			for (int i = 0; i < numReps / 4; i++) {
				byte[] digest = computeMd5(node.toString() + i);
				for (int h = 0; h < 4; h++) {
					long m = hash(digest, h);
					ketamaNodes.put(m, node);
				}
			}
		}
	}

	public T getPrimary(final String k)
	{
		byte[] digest = computeMd5(k);
		T rv = getNodeForKey(hash(digest, 0));
		return rv;
	}
	public T getPrimary(final byte[] kBytes)
	{
		byte[] digest = computeMd5(kBytes);
		T rv = getNodeForKey(hash(digest, 0));
		return rv;
	}

	T getNodeForKey(long hash)
	{
		final T rv;
		Long key = hash;
		if (!ketamaNodes.containsKey(key)) {
			// SortedMap<Long, T> tailMap=ketamaNodes.tailMap(key);
			// if(tailMap.isEmpty()) {
			// key=ketamaNodes.firstKey();
			// } else {
			// key=tailMap.firstKey();
			// }
			// For JDK1.6 version
			key = ketamaNodes.ceilingKey(key);
			if (key == null) {
				key = ketamaNodes.firstKey();
			}
		}

		rv = ketamaNodes.get(key);
		return rv;
	}

	private long hash(byte[] digest, int nTime)
	{
		long rv = ((long) (digest[3 + nTime * 4] & 0xFF) << 24) | ((long) (digest[2 + nTime * 4] & 0xFF) << 16)
				| ((long) (digest[1 + nTime * 4] & 0xFF) << 8) | (digest[0 + nTime * 4] & 0xFF);

		return rv & 0xffffffffL; /* Truncate to 32-bits */
	}

	/**
	 * Get the md5 of the given key.
	 */
	private byte[] computeMd5(String k)
	{
		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("MD5 not supported", e);
		}
		md5.reset();
		byte[] keyBytes = null;
		try {
			keyBytes = k.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unknown string :" + k, e);
		}

		md5.update(keyBytes);
		return md5.digest();
	}
	
	/**
	 * Get the md5 of the given key.
	 */
	private byte[] computeMd5(byte[] keyBytes)
	{
		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("MD5 not supported", e);
		}
		md5.reset();

		md5.update(keyBytes);
		return md5.digest();
	}

}
