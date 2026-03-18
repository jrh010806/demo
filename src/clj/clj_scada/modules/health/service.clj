(ns clj-scada.modules.health.service
  (:require [clj-scada.common.time-utils :as time-utils]))

(defn build-health-data
  "构建健康检查返回值。"
  []
  {:service "clj-scada"
   :status "ok"
   :timestamp (time-utils/now-ms)})

(defn healthy?
  "校验健康数据是否符合预期。"
  [health-data]
  (and (= "ok" (:status health-data))
       (string? (:service health-data))
       (integer? (:timestamp health-data))))
