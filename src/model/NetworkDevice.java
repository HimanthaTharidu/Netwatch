
package model;

public class NetworkDevice
{
    private final String name;
    private final String address;
    private final int port;

    private boolean online;
    private boolean checked;
    private long responseTime;
    private int failureCount;

    public NetworkDevice(String name, String address, int port)
    {
        this.name = name;
        this.address = address;
        this.port = port;

        online = false;
        checked = false;
        responseTime = -1;
        failureCount = 0;
    }

    public String getName()
    {
        return name;
    }

    public String getAddress()
    {
        return address;
    }

    public int getPort()
    {
        return port;
    }

    public synchronized boolean isOnline()
    {
        return online;
    }

    public synchronized boolean isChecked()
    {
        return checked;
    }

    public synchronized long getResponseTime()
    {
        return responseTime;
    }

    public synchronized int getFailureCount()
    {
        return failureCount;
    }

    public synchronized String updateStatus(boolean reachable, long duration)
    {
        boolean previousOnline = online;
        boolean previouslyChecked = checked;

        if (reachable)
        {
            failureCount = 0;
            online = true;
            responseTime = duration;
            checked = true;
        }
        else
        {
            failureCount++;

            if (failureCount >= 2)
            {
                online = false;
                responseTime = -1;
                checked = true;
            }
        }

        if (!previouslyChecked && checked)
        {
            return "INITIAL";
        }

        if (previouslyChecked && previousOnline && !online)
        {
            return "OFFLINE";
        }

        if (previouslyChecked && !previousOnline && online)
        {
            return "RECOVERED";
        }

        return "UNCHANGED";
    }
}
