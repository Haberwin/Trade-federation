package com.android.tradefed.maidu;

import com.android.tradefed.config.Option;
import com.android.tradefed.testtype.ITestFilterReceiver;
import com.android.tradefed.testtype.InstrumentationTest;
import com.android.tradefed.util.TestFilterHelper;

import java.util.HashSet;
import java.util.Set;


public class WeeklyCheckTest extends InstrumentationTest implements ITestFilterReceiver {

    private TestFilterHelper mFilterHelper;

    /** The include filters of the test name to run */
    @Option(name = "include-filter",
            description="The include filters of the test name to run.")
    protected Set<String> mIncludeFilters = new HashSet<>();

    /** The exclude filters of the test name to run */
    @Option(name = "exclude-filter",
            description="The exclude filters of the test name to run.")
    protected Set<String> mExcludeFilters = new HashSet<>();


    /**
     * {@inheritDoc}
     */
    @Override
    public void addIncludeFilter(String filter) {
        mIncludeFilters.add(filter);
        mFilterHelper.addIncludeFilter(filter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAllIncludeFilters(Set<String> filters) {
        mIncludeFilters.addAll(filters);
        mFilterHelper.addAllIncludeFilters(filters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addExcludeFilter(String filter) {
        mExcludeFilters.add(filter);
        mFilterHelper.addExcludeFilter(filter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAllExcludeFilters(Set<String> filters) {
        mExcludeFilters.addAll(filters);
        mFilterHelper.addAllExcludeFilters(filters);
    }

}