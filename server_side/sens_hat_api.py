import ssl
from flask import Flask, jsonify, make_response
from sense_hat import SenseHat

app = Flask(__name__)
# Init sense hat
sense = SenseHat()
sense.low_light = True
red = (255, 0, 0)
orange = (255, 165, 0)
yellow = (255, 255, 0)
green = (0, 255, 0)
blue = (0, 0, 255)
purple = (160, 32, 240)


# Set only compass
# sense.set_imu_config(True, False, False)  # gyroscope only

# Write Hello and draw smily face
@app.route("/")
def home():
    sense.show_message("Hello", text_colour=green, back_colour=blue, scroll_speed=0.3)
    sense.set_pixel(2, 2, (0, 0, 255))
    sense.set_pixel(4, 2, (0, 0, 255))
    sense.set_pixel(3, 4, (100, 0, 0))
    sense.set_pixel(1, 5, (255, 0, 0))
    sense.set_pixel(2, 6, (255, 0, 0))
    sense.set_pixel(3, 6, (255, 0, 0))
    sense.set_pixel(4, 6, (255, 0, 0))
    sense.set_pixel(5, 5, (255, 0, 0))

    return jsonify({
        "message": "Start"
    })


# Set led background blue
@app.route("/api/set_bg_blue", methods=['GET'])
def set_bg_blue():
    sense.show_message("", text_colour=green, back_colour=blue, scroll_speed=0.1)

    resp = make_response({
        "message": "Set background to blue"
    })

    return resp


# Set led background yellow
@app.route("/api/set_bg_yellow", methods=['GET'])
def set_bg_yellow():
    sense.show_message("", text_colour=green, back_colour=yellow, scroll_speed=0.1)

    resp = make_response({
        "message": "Set background to yellow"
    })

    return resp


# Set led background red
@app.route("/api/set_bg_red", methods=['GET'])
def set_bg_red():
    sense.show_message("", text_colour=green, back_colour=red, scroll_speed=0.1)
    resp = make_response(jsonify({"message": "Set background to red"}))

    return resp


# Gets the direction of North from the magnetometer in degree
@app.route("/api/get_north", methods=['GET'])
def get_north():
    north = sense.get_compass()
    sense.show_message("N", text_colour=green, back_colour=blue)
    resp = make_response(jsonify(north))

    return resp


# Get temperature in Celsius
@app.route("/api/get_temperature", methods=['GET'])
def get_temperature():
    temp = sense.get_temperature()
    sense.show_message("T", text_colour=green, back_colour=blue)

    resp = make_response(jsonify(temp))

    return resp


# Get temperature in Celsius from humidity sensor
@app.route("/api/get_temperature-from-humidity", methods=['GET'])
def get_temperature_from_humidity():
    temp = sense.ense.get_temperature_from_humidity()
    sense.show_message("TH", text_colour=green, back_colour=blue)

    resp = make_response(jsonify(temp))

    return resp


# get temperature in Celsius from pressure sensor
@app.route("/api/get_temp_from_pressure", methods=['GET'])
def get_temp_from_pressure():
    temp = sense.get_temperature_from_pressure()
    sense.show_message("TP", text_colour=green, back_colour=blue)

    resp = make_response(jsonify(temp))

    return resp


# get humidity
@app.route("/api/get_humidity", methods=['GET'])
def get_humidity():
    humidity = sense.get_humidity()
    sense.show_message("H", text_colour=green, back_colour=blue)

    resp = make_response(jsonify(humidity))

    return resp


# Gets air pressure in Millibars
@app.route("/api/get_pressure", methods=['GET'])
def get_pressure():
    pressure = sense.get_pressure()
    sense.show_message("P", text_colour=green, back_colour=blue)

    resp = make_response(jsonify(pressure))

    return resp


# Clear LED lights
@app.route("/api/clear_lights", methods=['GET'])
def clear_lights():
    sense.clear()
    return jsonify({
        "message": "LED lights cleared"
    })


if __name__ == "__main__":

    app.run(host='0.0.0.0', port=8000)