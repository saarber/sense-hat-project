#!/bin/bash
nohup python3 -m gunicorn -c /home/[user-Name]/sense-hat-project/server_side/gunicorn.conf.py sens_hat_api:app &