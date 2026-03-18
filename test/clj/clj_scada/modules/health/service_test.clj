(ns clj-scada.modules.health.service-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-scada.modules.health.service :as health-service]))

(deftest build-health-data-test
  (testing "健康检查数据结构完整"
    (let [health-data (health-service/build-health-data)]
      (prn "你好")
      (is (= "clj-scada" (:service health-data)))
      (is (= "ok" (:status health-data)))
      (is (integer? (:timestamp health-data))))))

(deftest healthy?-test
  (testing "健康检查状态判断正确"
    (is (true? (health-service/healthy?
                {:service "clj-scada"
                 :status "ok"
                 :timestamp 1})))
    (is (false? (health-service/healthy?
                 {:service "clj-scada"
                  :status "down"
                  :timestamp 1})))))
