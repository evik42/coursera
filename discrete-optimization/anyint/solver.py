#!/usr/bin/python
# -*- coding: utf-8 -*-
import os

def solve_it(input_data):
    return os.popen("java -jar ../../out/artifacts/discrete-optimization/anyint.jar").read()

if __name__ == '__main__':
    print('This script submits the integer: %s\n' % solve_it(''))

