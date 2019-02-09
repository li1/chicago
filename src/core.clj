(ns chicago.core
  (:require [clj-3df.core :as df :use [exec!]]
            [chicago.diff-formatter :as formatter]))

(def schema
  {:thug/name      {:db/valueType :String}
   :thug/boss      {:db/valueType :Eid}
   :thug/earnings  {:db/valueType :Number}
   :thug/rat?      {:db/valueType :Bool}
   :thug/territory {:db/valueType :Eid}
   :territory/name {:db/valueType :String}})

(def db (df/create-db schema))

(def initial-data
  [{:db/id 1 :territory/name "west"}
   {:db/id          3
    :thug/name      "alfredo"
    :thug/earnings  1000
    :thug/territory 1}
   {:db/id          4
    :thug/name      "bernado"
    :thug/boss      3
    :thug/earnings  500
    :thug/territory 1}
   {:db/id          5
    :thug/name      "carlo"
    :thug/boss      4
    :thug/earnings  120
    :thug/territory 1}
   {:db/id          6
    :thug/name      "cristiano"
    :thug/boss      4
    :thug/earnings  100
    :thug/territory 1}

   {:db/id 2 :territory/name "east"}
   {:db/id          7
    :thug/name      "aurora"
    :thug/earnings  900
    :thug/territory 2}
   {:db/id          8
    :thug/name      "berta"
    :thug/boss      7
    :thug/earnings  450
    :thug/territory 2}
   {:db/id          9
    :thug/name      "carla"
    :thug/boss      8
    :thug/earnings  125
    :thug/territory 2}
   {:db/id          10
    :thug/name      "corinna"
    :thug/boss      8
    :thug/earnings  125
    :thug/territory 2}])

(def territory-earnings
  '[:find ?territory (sum ?earnings)
    :with ?t
    :where
    [?terr :territory/name ?territory]
    [?t :thug/territory ?terr]
    [?t :thug/earnings ?earnings]])

(def sub-earnings
  '[:find ?b ?boss (sum ?earnings)
    :with ?t
    :where
    [?b :thug/name ?boss]
    [?t :thug/earnings ?earnings]
    (subordinate? ?t ?b)])

(def rules
  '[;; read `->` as "boss of" relation: boss -> subordinate
    ;; let A -> B -> C
    ;; then (subordinate? C B) == true
    ;; and  (subordinate? A B) == false
    [(subordinate? ?sub ?boss)
     [?sub :thug/boss ?boss]]

    [(subordinate? ?sub ?boss)
     [?sub :thug/boss ?middleman]
     (subordinate? ?middleman ?boss)]])

;; note: we couldn't build thug-earnings with a sub-earnings rule,
;; since those don't support aggregates
(def thug-total-earnings
    '[:find ?t ?thug ?earnings
      :where
      (or (and [?t :thug/name ?thug]
               [?t :thug/earnings ?earnings]
               (not [?s :thug/boss ?t]))
          (and [?t :thug/name ?thug]
               [?t :thug/earnings ?thug-earnings]
               [chic/sub-earnings ?t ?thug ?sub-earnings]
               [(add ?thug-earnings ?sub-earnings) ?earnings]))])

(comment
;; in this first version, we're missing the lowest level
;; since sub-earnings is empty for them and the whole
;; query doesn't resolve and we don't support default values yet
  (def thug-earnings
    '[:find ?t ?thug ?earnings
      :where
      [?t :thug/name ?thug]
      [?t :thug/earnings ?thug-earnings]
      [chic/sub-earnings ?t ?thug ?sub-earnings]
      [(add ?thug-earnings ?sub-earnings) ?earnings]]))

(def rats
  '[:find ?t ?thug
    :where
    [?t :thug/rat? true]
    [?t :thug/name ?thug]])

(def rat-trigger
  '[:find ?t ?thug ?earnings
    :where
    [chic/thug-total-earnings ?t ?thug ?total-earnings]
    [?t :thug/earnings ?earnings]
    (not [chic/rats ?t ?thug])
    [(< ?total-earnings 1000)]
    [(> ?total-earnings 500)]])

(def squeak-trigger
  '[:find ?b ?boss ?earnings
    :where
    [?b :thug/name ?boss]
    [?b :thug/earnings ?earnings]
    (not [chic/rats ?b ?boss])
    [?s :thug/name ?sub]
    (subordinate? ?s ?b)
    [chic/rats ?s ?sub]])

(declare conn)
(defn turn-to-rat [desc prob diffs]
  (doseq [[[rat name earnings] _time diff] diffs]
    (let [is-rat (< (rand) prob)]
      (when (pos? diff)
        (println "is" name "a" desc "?" is-rat)
        (when is-rat
          (exec! conn (df/transact db [[:db/add rat :thug/rat? true]
                                       [:db/retract rat :thug/earnings earnings]
                                       [:db/add rat :thug/earnings 1]])))))))

(comment
  (do
    (def conn (df/create-publication "ws://127.0.0.1:6262" (comp clojure.pprint/pprint formatter/format-diffs)))
    (exec! conn (df/create-db-inputs db)))

  (exec! conn (df/transact db initial-data))

  (exec! conn (df/register-query db "chic/territory-earnings" territory-earnings))
  (exec! conn (df/register-query db "chic/sub-earnings" sub-earnings rules))
  (exec! conn (df/register-query db "chic/thug-total-earnings" thug-total-earnings))
  (exec! conn (df/register-query db "chic/rats" rats))

  (df/business-rule conn db "chic/might-rat" rat-trigger (partial turn-to-rat "rat" 0.5))

  (df/business-rule conn db "chic/might-squeak" squeak-trigger (partial turn-to-rat "squeaker" 0.8))

  (exec! conn (df/transact db [[:db/retract 10 :thug/earnings 125]
                               [:db/add 10 :thug/earnings 550]]))
)
