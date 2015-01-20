/*
 * FAE, Feinno App Engine
 *  
 * Create by 李会军 2011-1-20
 * 
 * Copyright (c) 2011 北京新媒传信科技有限公司
 */
package test.com.feinno.rpc.servermock;

import com.feinno.serialization.protobuf.ProtoEntity;
import com.feinno.serialization.protobuf.ProtoMember;

/**
 * AddOutputArgs
 * 
 * @author 李会军
 */
public class AddOutputArgs extends ProtoEntity {

	@ProtoMember(1)
	private int z;
	
	public int getZ(){
		return z;
	}
	
	public AddOutputArgs setZ(int value){
		z = value;
		return this;
	}
}
