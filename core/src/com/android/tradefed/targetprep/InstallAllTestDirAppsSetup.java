/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tradefed.targetprep;

import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.config.Option;
import com.android.tradefed.config.OptionClass;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.util.AaptParser;
import com.android.tradefed.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@link ITargetPreparer} that installs all apps in a test zip. For individual test app install
 * please look at {@link TestAppInstallSetup}.
 */
@OptionClass(alias = "all-tests-dir-installer")
public class InstallAllTestDirAppsSetup extends BaseTargetPreparer implements ITargetCleaner {
    @Option(
        name = "install-arg",
        description =
                "Additional arguments to be passed to install command, "
                        + "including leading dash, e.g. \"-d\""
    )
    private Collection<String> mInstallArgs = new ArrayList<>();

    @Option(
        name = "cleanup-apks",
        description =
                "Whether apks installed should be uninstalled after test. Note that the "
                        + "preparer does not verify if the apks are successfully removed."
    )
    private boolean mCleanup = true;

    @Option(
        name = "stop-install-on-failure",
        description =
                "Whether to stop the preparer by throwing an exception or only log the "
                        + "error on continue."
    )
    private boolean mStopInstallOnFailure = true;

    @Option(name = "test-dir-name", description = "Dir name containing APKs.")
    private String mTestDirName;

    List<String> mPackagesInstalled = new ArrayList<>();

    public void setTestDirName(String testDirName) {
        mTestDirName = testDirName;
    }

    public void setStopInstallOnFailure(boolean stopInstallOnFailure) {
        mStopInstallOnFailure = stopInstallOnFailure;
    }

    public void setCleanup(boolean cleanup) {
        mCleanup = cleanup;
    }

    /** {@inheritDoc} */
    @Override
    public void setUp(ITestDevice device, IBuildInfo buildInfo)
            throws TargetSetupError, DeviceNotAvailableException {
        if (isDisabled()) {
            CLog.d("InstallAllTestDirAppsSetup disabled, skipping setUp");
            return;
        }
        File testsDir = getDirFile(device, buildInfo);
        // Locate test dir where the test dir file was unzip to.

        if (testsDir == null) {
            throw new TargetSetupError(
                    "Failed to find a valid test directory.", device.getDeviceDescriptor());
        }

        try {
            installApksRecursively(testsDir, device);
        } finally {
            //FileUtil.recursiveDelete(testsDir);
        }
    }

    /**
     * Returns the dir file.
     *
     * @param buildInfo {@link IBuildInfo} containing files.
     * @return the {@link File} for the dir file.
     */
    File getDirFile(ITestDevice device, IBuildInfo buildInfo) throws TargetSetupError {

        if (mTestDirName == null) {
            throw new TargetSetupError("test-dir-name is null.", device.getDeviceDescriptor());
        }
        File tempFile=new File(mTestDirName) ;
        CLog.d("liuwenhua:Dir path : %s",tempFile.getAbsolutePath());
        return tempFile.getAbsoluteFile();
        //return buildInfo.getFile(mTestDirName);
    }

    /**
     * Install all apks found in a given directory.
     *
     * @param directory {@link File} directory to install from.
     * @param device {@link ITestDevice} to install all apks to.
     * @throws TargetSetupError
     * @throws DeviceNotAvailableException
     */
    void installApksRecursively(File directory, ITestDevice device)
            throws TargetSetupError, DeviceNotAvailableException {
        if (directory == null || !directory.isDirectory()) {
            throw new TargetSetupError("Invalid test directory!", device.getDeviceDescriptor());
        }
        CLog.d("Installing all apks found in dir %s ...", directory.getAbsolutePath());
        File[] files = directory.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                installApksRecursively(f, device);
            } else if (FileUtil.getExtension(f.getAbsolutePath()).toLowerCase().equals(".apk")) {
                //CLog.i("Install %s at device %",f.list(),device);
                installApk(f, device);
            } else {
                CLog.d("Skipping %s because it is not an apk", f.getAbsolutePath());
            }
        }
    }


    /**
     * Installs a single app to the device.
     *
     * @param appFile {@link File} of the apk to install.
     * @param device {@link ITestDevice} to install the apk to.
     * @throws TargetSetupError
     * @throws DeviceNotAvailableException
     */
    void installApk(File appFile, ITestDevice device)
            throws TargetSetupError, DeviceNotAvailableException {
        CLog.i("Installing apk from %s ...", appFile.getAbsolutePath());
        String result = device.installPackage(appFile, true, mInstallArgs.toArray(new String[] {}));
        if (result == null) {
            // only consider cleanup if install was successful
            if (mCleanup) {
                addApkToInstalledList(appFile, device);
            }
        } else if (mStopInstallOnFailure) {
            // if flag is true, we stop the sequence for an exception.
            throw new TargetSetupError(
                    String.format(
                            "Failed to install %s on %s. Reason: '%s'",
                            appFile, device.getSerialNumber(), result),
                    device.getDeviceDescriptor());
        } else {
            CLog.e(
                    "Failed to install %s on %s. Reason: '%s'",
                    appFile, device.getSerialNumber(), result);
        }
    }

    /**
     * Adds an app to the list of apps to uninstall in tearDown() if necessary.
     *
     * @param appFile {@link File} of apk.
     * @param device {@link ITestDevice} apk was installed on.
     * @throws TargetSetupError
     */
    void addApkToInstalledList(File appFile, ITestDevice device) throws TargetSetupError {
        String packageName = getAppPackageName(appFile);
        if (packageName == null) {
            throw new TargetSetupError(
                    "apk installed but AaptParser failed", device.getDeviceDescriptor());
        }
        mPackagesInstalled.add(packageName);
    }

    /**
     * Returns the package name for a an apk. Returns null if any errors.
     *
     * @param appFile {@link File} of apk.
     * @return Package name of appFile, null if errors.
     */
    String getAppPackageName(File appFile) {
        AaptParser parser = AaptParser.parse(appFile);
        if (parser == null) {
            return null;
        }
        return parser.getPackageName();
    }

    /** {@inheritDoc} */
    @Override
    public void tearDown(ITestDevice device, IBuildInfo buildInfo, Throwable e)
            throws DeviceNotAvailableException {
        if (isDisabled()) {
            CLog.d("InstallAllTestdirAppsSetup disabled, skipping tearDown");
            return;
        }
        if (mCleanup && !(e instanceof DeviceNotAvailableException)) {
            for (String packageName : mPackagesInstalled) {
                String msg = device.uninstallPackage(packageName);
                if (msg != null) {
                    CLog.w(String.format("error uninstalling package '%s': %s", packageName, msg));
                }
            }
        }
    }
}
