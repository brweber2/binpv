;;;;
;;;;
;;;;   Clojure feels like a general-purpose language beamed back
;;;;     from the near future.
;;;;   -- Stu Halloway
;;;;
;;;;   Name:               dump.clj
;;;;
;;;;   Started:            Sun Nov 13 23:23:15 2011
;;;;   Modifications:
;;;;
;;;;   Purpose:
;;;;
;;;;
;;;;
;;;;   Calling Sequence:
;;;;
;;;;
;;;;   Inputs:
;;;;
;;;;   Outputs:
;;;;
;;;;   Example:
;;;;
;;;;   Notes:
;;;;
;;;;

(ns binpv.dump
  (:use [clojure.pprint :only (cl-format)]))
   
(def line-width 16)

(defn printable-char? [ch]
  (<= (int \space) (int ch) (int \~)))

(defn code->char [n]
  (let [ch (char n)]
    (if (printable-char? ch)
      ch
      \.)))

(defn output [byte-count line]
  (let [mid-point (dec (quot line-width 2))]
    (cl-format true "~:@(~8,'0X~):  " byte-count)
    (dotimes [i line-width]
      (cl-format true "~:@(~2,'0X~)~:[ ~;-~]" (nth line i) (= i mid-point)))
    (cl-format true " ")
    (dotimes [i line-width]
      (cl-format true "~C~:[~;-~]" (code->char (nth line i)) (= i mid-point)))
    (cl-format true "~%")))

(defn fill-line [line]
  (let [fill (- line-width (count line))]
    (if (zero? fill)
      line
      (concat line (repeat fill 0)))) )
              
(defn dump-buffer [buffer]
  (loop [buff (drop line-width buffer)
         line (fill-line (take line-width buffer))
         byte-count 0]
    (output byte-count line)
    (when-not (empty? buff)
      (recur (drop line-width buff) (fill-line (take line-width buff)) (+ byte-count line-width)))) )

