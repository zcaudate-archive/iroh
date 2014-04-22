(ns iroh.pretty.args
  (:require [iroh.common :refer :all]
            [iroh.pretty.primitives :refer [primitive-convert]]
            [iroh.pretty.classes :refer [class-convert]]))

(def sort-terms #{:by-name :by-params :by-modifiers :by-type})

(def select-terms #{:tag
                    :name
                    :params
                    :modifiers
                    :type
                    :attributes
                    :origins
                    :container
                    :delegate})

(defn args-classify
  "args-classify
  (map (fn [[i x]] [i (args-classify x)])
       {0  :by-name     ;; sort - :by-params, :by-modifiers, :by-type
        1  :tag         ;; display - :name, :params, :modifiers, :type, :attributes,
                        ;;           :origins, :container, :delegate
        2  :first       ;; gets the first element
        3  :#           ;; merge all elements into a single multi element
        4  \"toString\"   ;; matches exact name of function
        5  #\"to*\"       ;; matches name containing regex
        6  #(-> % :type (= :field))  ;; matches on predicate element
        7  #{Class}     ;; match origin of element
        8  [:any 'int]  ;; match any parameter type
        9  [:all 'int 'long] ;; match all parameter types
        10 ['byte 'byte] ;; match exact paramter types
        11 3             ;; match number of parameters
        13 'int          ;; match on the type of element
        14 :public       ;; match on modifiers (:public, :static, etc...)
        })
  => [[0 :sort-terms] [1 :select-terms] [2 :first] [3 :merge] [4 :name]
      [5 :name] [6 :predicate] [7 :origins] [8 :any-params] [9 :all-params]
      [10 :params] [11 :num-params] [13 :type] [14 :modifiers]]"
  {:added "0.1.10"}
  [arg]
  (cond (sort-terms arg)                 :sort-terms
        (select-terms arg)               :select-terms
        (= :first arg)                   :first
        (or (= :# arg) (= :merge arg))   :merge

        (or (string? arg) (regex? arg))  :name
        (fn? arg)                        :predicate
        (set? arg)                       :origins
        (and (vector? arg)
             (= :any (first arg)))       :any-params
        (and (vector? arg)
             (= :all (first arg)))       :all-params
        (number? arg)                    :num-params
        (vector? arg)                    :params

        (or (class? arg) (symbol? arg))  :type

        (keyword? arg)                   :modifiers
        (hash-map? arg)                  :attribute))

(defn args-convert
  "args-convert"
  {:added "0.1.10"}
  [args]
  (let [single-fn #(or (class-convert %) %)]
  (mapv (fn [v]
          (cond (vector? v)
                (mapv single-fn v)

                (set? v)
                (set (map single-fn v))

                (symbol? v)
                (or (primitive-convert v :symbol :class)
                    v)
                :else v))
        args)))

(defn args-group
  "args-group"
  {:added "0.1.10"}
  [args]
  (-> (group-by args-classify args)
      (update-in-if [:params] args-convert)
      (update-in-if [:type] args-convert)))
