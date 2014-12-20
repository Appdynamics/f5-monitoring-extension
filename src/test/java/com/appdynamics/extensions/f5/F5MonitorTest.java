package com.appdynamics.extensions.f5;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Maps;

public class F5MonitorTest {
	
	private F5Monitor classUnderTest = new F5Monitor();
	
	@Test
	public void test() throws Exception {
		Map<String, String> args = Maps.newHashMap();
		args.put("config-file","src/test/resources/conf/config.yaml");
		
		// TODO:
//		classUnderTest.execute(args, null);
	}

}
