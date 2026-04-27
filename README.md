# Raspberry Pi Sense HAT Dashboard

An open-source Raspberry Pi Sense HAT project that exposes live sensor data through a small Flask API and displays it in a web dashboard. The project also includes a native Android client app that uses the same API.

## What This Project Does

This project reads data from a Raspberry Pi Sense HAT and makes it available to client applications. The current clients show:

- Temperature
- Humidity
- Air pressure
- Compass north heading
- Source status
- Last update time

The web dashboard and Android app are designed as reusable client examples. Update the placeholder project name, source labels, API URLs, and deployment settings before publishing your own version.

## Project Structure

```text
.
+-- client_side/
|   +-- index.html
|   +-- styles.css
|   +-- sensors.js
|   +-- senseit-config.js
+-- server_side/
|   +-- sens_hat_api.py
|   +-- gunicorn.conf.py
|   +-- nginx.conf
|   +-- start-server.sh
|   +-- stop-server.sh
|   +-- start-senseit-server.service
+-- android_client/
|   +-- README.md
|   +-- settings.gradle
|   +-- build.gradle
|   +-- app/
+-- requirment_Install_on_rpi.txt
```

## Backend API

The backend is a Flask app in `server_side/sens_hat_api.py`. It reads values from the Sense HAT and returns JSON responses.

Main API routes:

- `GET /api/get_temperature`
- `GET /api/get_humidity`
- `GET /api/get_pressure`
- `GET /api/get_north`
- `GET /api/clear_lights`

There are also LED/background helper routes in the backend file.

## Web Client

The browser client is in `client_side/`.

- `index.html` contains the dashboard structure.
- `styles.css` contains the visual design.
- `sensors.js` polls the configured API sources and updates the page.
- `senseit-config.js` contains editable client-side settings such as API URLs, source labels, refresh interval, and timeout.

Default source URLs are configured like this:

```js
window.SENSEIT_SOURCES = [
  {
    key: 'source-a',
    label: 'Sense HAT PI A - Living Room',
    baseUrl: 'https://sensors.example.com/sensehat-a'
  },
  {
    key: 'source-b',
    label: 'Sense HAT PI B - Studio',
    baseUrl: 'https://sensors.example.com/sensehat-b'
  }
];
```

Update these URLs to match your own server or Raspberry Pi deployment.

## Android App Included

This repository also includes a native Android client app in `android_client/`.

The Android app:

- Uses the same API routes as the web client.
- Shows the same sensor values.
- Includes manual refresh and auto-refresh.
- Has an in-app settings section for API URLs, labels, refresh interval, and timeout.
- Saves settings locally on the device.
- Uses a visual style based on the current website.

See `android_client/README.md` for Android-specific build and run instructions.

## Raspberry Pi Setup

Install the required Python packages on the Raspberry Pi:

```bash
sudo apt-get install python3-flask
sudo apt-get install python3-gunicorn
```

The project expects the Raspberry Pi Sense HAT Python library to be available on the device.

Start the backend directly for development:

```bash
cd server_side
python3 sens_hat_api.py
```

The API runs on port `8000` by default.

## Production Deployment

The `server_side/` folder includes example files for deployment:

- `gunicorn.conf.py` for running the Flask app with Gunicorn.
- `nginx.conf` for serving the web client and proxying Sense HAT API sources.
- `start-server.sh` and `stop-server.sh` helper scripts.
- `start-senseit-server.service` for systemd service setup.

Review and update all paths, domain names, certificate paths, usernames, and IP addresses before using these files on a real server.

## Customization Checklist

Before publishing or deploying your own version:

- Replace placeholder project name and description.
- Update API URLs in `client_side/senseit-config.js`.
- Update default API URLs in the Android app if needed.
- Replace example domain names in Nginx and Gunicorn config files.
- Add your preferred license.
- Add your repository link.
- Test the backend on a Raspberry Pi with a real Sense HAT attached.

## License

Add your project license here before publishing.
