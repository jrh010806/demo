(ns clj-scada.core
  (:require [clj-scada.modules.health.handler :as health-handler]))

(defn routes
  "返回示例路由定义。"
  []
  [{:uri "/api/health"
    :method :get
    :handler health-handler/health-response}])

(defn app-info
  "返回应用基础信息。"
  []
  {:name "clj-scada"
   :status :booting
   :routes-count (count (routes))})
