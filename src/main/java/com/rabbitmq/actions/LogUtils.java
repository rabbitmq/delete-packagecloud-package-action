/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.rabbitmq.actions;

abstract class LogUtils {

  private LogUtils() {}

  static void logGreen(String message) {
    log(green(message));
  }

  static String green(String message) {
    return "\u001B[32m" + message + "\u001B[0m";
  }

  static void logYellow(String message) {
    log(yellow(message));
  }

  static String yellow(String message) {
    return "\u001B[33m" + message + "\u001B[0m";
  }

  static void logRed(String message) {
    log(red(message));
  }

  static String red(String message) {
    return "\u001B[31m" + message + "\u001B[0m";
  }

  static void log(String message, Object... args) {
    System.out.printf((message) + "%n", args);
  }

  static void newLine() {
    log("");
  }

  static void logIndent(String message) {
    log(indent(message));
  }

  static String indent(String message) {
    return "    " + message;
  }
}
