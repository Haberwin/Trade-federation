package com.android.tradefed.maidu;

import java.util.Collections;

import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.result.TestDescription;
import com.android.tradefed.testtype.IDeviceTest;
import com.android.tradefed.testtype.IRemoteTest;

public class WeeklyCheckTest implements IRemoteTest, IDeviceTest {
	private ITestDevice mDevice;

	@Override
	public void setDevice(ITestDevice device) {
		// TODO Auto-generated method stub
		mDevice=device;

	}

	@Override
	public ITestDevice getDevice() {
		// TODO Auto-generated method stub
		return mDevice;
	}

	@Override
	public void run(ITestInvocationListener listener) throws DeviceNotAvailableException {
		// TODO Auto-generated method stub
		System.out.println("Hellow,Weekly Check! I have device" + getDevice().getSerialNumber());
		TestDescription testId=new TestDescription("com.maidu.WeeklyCheckTest","weeklyCheck");
		listener.testRunStarted("weeklyrun", 1);
		listener.testStarted(testId);
		listener.testFailed(testId, "Weekly test Failed");
		listener.testEnded(testId, Collections.emptyMap());
		listener.testRunEnded(0, Collections.emptyMap());
		

	}

}