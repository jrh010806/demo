(ns clj-scada.common.time-utils)

(defn now-ms
  "返回当前时间戳，单位毫秒。"
  []
  (System/currentTimeMillis))

(defn fresh-window?
  "判断给定时间戳是否仍在有效窗口内。111"
  [timestamp-ms window-ms]
  (<= (- (now-ms) timestamp-ms) window-ms))