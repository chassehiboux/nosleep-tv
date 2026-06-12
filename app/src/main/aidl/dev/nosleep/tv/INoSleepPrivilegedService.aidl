package dev.nosleep.tv;

interface INoSleepPrivilegedService {
    void destroy() = 16777114;

    int forceStopPackage(String packageName) = 1;
}
