
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
- set endpoints ip + port or url into config 
- create a virtual env and install pytest with pip install pytest

On Snap4City:
- set main account and secondary account tokens into config vars,
- set model, type, contextbroker, producer, subnature, organization into variables in test_insert.py and in test_get.py,
- comment/decomment the request version (ip+port or url) based on your setup

## Run
- to run tests single file: pytest path/to/test/file.py
- to run all tests: pytest path/to/folder/
- to run specific test: pytest path/to/test/file.py -k test_function_name

