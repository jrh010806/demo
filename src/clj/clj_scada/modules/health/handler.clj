(ns clj-scada.modules.health.handler
  (:require [clj-scada.modules.health.service :as health-service]))

(defn health-response
  "返回示例 Ring 风格响应。"
  []
  {:status 200
   :headers {"content-type" "application/json; charset=utf-8"}
   :body (health-service/build-health-data)})
