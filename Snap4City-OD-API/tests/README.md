
# OD Endpoint tests

This is a quick guide to setup and start tests



## Content
- python files:
    - data: some sample data
    - functions: the functions to be tested
    - tests: pytest tests
- config.json: configuration file

## Setup
- ensure endpoints containers are running
- get endpoints containers ip and set in config.json ip variable
- create a virtual env and install pytest with pip install pytest

On Snap4City:
- login as secondary account, create and fill up a device,
- set model, type, contextbroker, producer, subnature, organization into variables in test_insert.py and test_get.py,
- set its od_id and organization into all test_insert_xxxx_device_no_ownership variables in test_insert.py
- set its data into all test_get_xxxx_resource_no_ownership variables in test_get.py,
- get main account token and set it into expired_token_header Authorization in test_build.py,
- get another token and set it into config.json token

## Run
- to run tests single file: pytest path/to/test/file.py
- to run all tests: pytest path/to/folder/
- to run specific test: pytest path/to/test/file.py -k test_function_name

