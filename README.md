# NetWatch

## Project Description
NetWatch is a Java network monitoring application with a Swing dashboard and a TCP server. It checks configured network devices for reachability, displays their status and response time, and records status changes and device management events.

## Features
- Monitor configured IP addresses with ping-style reachability checks or TCP port probes.
- Add and remove devices from the dashboard.
- Display device status and probe response time.
- Alert when a monitored device goes offline or recovers.
- View recent server logs.
- Save the device list and logs between server runs.
- Reconnect the dashboard when its connection to the server is interrupted.

## Project Structure
- `src/client/` — Swing dashboard, TCP client, and alert display (`Dashboard`, `NetWatchClient`, `AlertManager`).
- `src/model/` — per-device monitoring state and transition logic (`NetworkDevice`).
- `src/server/` — TCP server, client command handling, device registry, monitoring, persistence, logging, and test listener.
- `src/data/devices.txt` — saved device entries.
- `src/data/network.log` — server log file.

## Technologies Used
- Java and the standard Java networking, I/O, concurrency, and time APIs.
- Swing for the desktop dashboard.
- TCP sockets for dashboard/server communication and TCP service probes.
- `InetAddress.isReachable` for port `0` reachability checks.
- UTF-8 text files for device and log persistence.

## Requirements
- JDK 11 or newer.
- A Java IDE or Java compiler/runtime.
- Network access between the server and devices being monitored. No third-party libraries or build tool are required by the current source tree.

## Installation
1. Open the project folder in a Java IDE.
2. Configure the project to use JDK 11 or newer and mark `src` as a source folder if the IDE does not do so automatically.
3. Create run configurations for `server.NetWatchServer` and `client.Dashboard`.
4. If you want to test a TCP probe, also create a run configuration for `server.TestService`.

## Configuration
The server listens on TCP port `5000`. The dashboard connects to `127.0.0.1:5000` by default. To connect a dashboard on another computer, set its VM options to `-Dnetwatch.host=<server-IP> -Dnetwatch.port=5000` and allow inbound TCP port `5000` through the server computer's firewall.

By default, persistent files are stored in `src/data/devices.txt` and `src/data/network.log`, located from the project or compiled class location rather than the current working directory. On first startup, non-empty files from `%USERPROFILE%\.netwatch` or the legacy working-directory `data` and `logs` folders are copied when the project destination file is missing or empty. Original files are kept. To choose another storage folder, set the server VM option `-Dnetwatch.home=<folder>`; it will contain both files.

## Running the Application
1. Start `server.NetWatchServer`.
2. Start `client.Dashboard`.
3. Optionally start `server.TestService` on a computer you want to probe over TCP; it listens on port `6000`.

Keep the server running while using the dashboard. The dashboard retries its connection if the server is not available when the dashboard starts.

## Usage
In the dashboard, select **Add Device** and enter a name, IP address, and port. Select a device row and choose **Remove Device** to remove it. Choose **View Logs** to retrieve recent server logs.

- **Wi-Fi router:** On the computer running `NetWatchServer`, run `ipconfig` and find **Default Gateway** under the active Wi-Fi adapter. Add that IP with port `0`, for example `192.168.1.1`. Port `0` checks whether the router responds to ping. Some routers block ping, so an offline result does not always mean Wi-Fi is disconnected.
- **Another laptop:** Connect both computers to the same local network and find the monitored laptop's Wi-Fi IPv4 address with `ipconfig`. Add it with port `0` for a ping check, or run `server.TestService` on that laptop and add its IP with port `6000`. Allow ping or inbound TCP port `6000` through that laptop's firewall as needed.
- **Localhost:** `127.0.0.1` refers to the computer running `NetWatchServer`. Add it with port `0` for a ping check, or run `server.TestService` on the server computer and use port `6000`. The server may already include a saved `Localhost` entry.

Monitoring checks run from the server computer, not the computer displaying the dashboard. Checks run on a five-second fixed delay after each cycle; unreachable devices require two failed checks before being marked offline. Port `0` uses a ping-style check; any non-zero port checks whether a TCP service is accepting connections. The test listener verifies that a port is reachable; it does not measure Wi-Fi signal strength or speed.

## Communication Protocol
The dashboard and server exchange UTF-8 text messages, one message per line. Commands include:

- `GET_STATUS`
- `GET_LOGS`
- `ADD_DEVICE|name|address|port`
- `REMOVE_DEVICE|name|address`
- `EXIT`

Server responses and broadcasts include `DEVICE|name|address|status|responseTimeMs`, `END_SNAPSHOT`, `ADDED|name|address|port`, `REMOVED|name|address`, `ALERT|name|message`, `LOG|entry`, `SUCCESS|message`, and `ERROR|message`. The server sends a device snapshot when a client connects and after `GET_STATUS`. Device names and addresses should not contain the `|` separator.

## Testing

NetWatch includes `TestService.java`, a TCP test utility located in `src/server/`, which is used to manually test device availability, offline detection, and recovery alerts. Additional manual testing covers device persistence, automatic server reconnection, monitoring another laptop, and system logging.

### 1. Local Device Monitoring

1. Open the project in Eclipse IDE.
2. Run `TestService.java` from `src/server/` to start the TCP test service on port `6000`.
3. Run `NetWatchServer.java` from `src/server/` to start the NetWatch server on port `5000`.
4. Run `Dashboard.java` from `src/client/` to open the monitoring dashboard.
5. Add a device with IP address `127.0.0.1` and port `6000` if it is not already registered.
6. Verify that the device appears **ONLINE**.
7. Stop `TestService.java` and verify that the device changes to **OFFLINE** with an alert.
8. Restart `TestService.java` and verify that a recovery alert appears.

### 2. Device Persistence

1. Add a new device through the dashboard.
2. Restart the NetWatch server.
3. Verify that the device remains registered.
4. Remove the test device after verification.

### 3. Automatic Server Reconnection

1. Start `NetWatchServer.java` and `Dashboard.java`.
2. Stop the NetWatch server while keeping the dashboard open.
3. Verify that a disconnection notification appears.
4. Restart the server.
5. Verify that the dashboard reconnects automatically.

### 4. Monitoring Another Laptop

1. Connect both laptops to the same network.
2. Find the second laptop's IPv4 address using `ipconfig`.
3. On the second laptop, open PowerShell and start a TCP listener on port `6000`:

   ```powershell
   $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Any, 6000)
   $listener.Start()
   ```

4. On the main laptop, verify TCP connectivity:

   ```powershell
   Test-NetConnection <Laptop2-IP> -Port 6000
   ```

5. Add the second laptop to NetWatch using its IPv4 address and port `6000`.
6. Verify that the device appears **ONLINE**.
7. On the second laptop, stop the TCP listener:

   ```powershell
   $listener.Stop()
   ```

8. Verify that NetWatch detects the device as **OFFLINE** and displays an alert.

**Note:** Host reachability checks using port `0` may fail if ICMP traffic is blocked by a firewall or network configuration. TCP monitoring can be used when the target service is accessible. Test only authorized devices.

### 5. Monitoring Logs

1. Start the NetWatch server and dashboard.
2. Generate monitoring events by adding devices or changing their availability.
3. Click **View Logs** on the dashboard.
4. Verify that device additions, removals, status changes, alerts, and recovery events are recorded.

### Test Results and Evidence

Detailed functional test cases, expected results, actual results, and screenshot evidence are available in docs/testing/.

## Known Limitations
- Ping responses can be blocked by operating systems, firewalls, or routers.
- A TCP probe confirms that a service is listening on the selected port; it does not confirm all functions of that device are healthy.
- `TestService` is only a test listener on port `6000`; it does not monitor a laptop's Wi-Fi signal or bandwidth.
- Probes run sequentially with a timeout, so monitoring cycles may take longer as more devices are added.
- The text protocol has no authentication or encryption and uses `|` separators; it is intended for trusted networks and simple project use.
- No license file or license declaration is currently included.

