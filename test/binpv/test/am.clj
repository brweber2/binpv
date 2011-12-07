(ns binpv.test.am
  (:use [binpv.core])
  (:use [binpv.dump])
  (:import [binpv.core ByteBasedChunker FixedLength EnumeratedValue DependentValue FixedLength DependentFixedLength VariableLength FileStreamWrapper HexVisualizer IntegerVisualizer DateVisualizer BinaryProtocolVisualizer StopAt])
  (:use [clojure.test]))

; SPECIFIC TO OUR IMPL

(defn get-udx [i]
  (case i
    20480 "foo"
    20481 "bar"
    20482 "baz"
    20483 "bleh"
    20484 "bing"
    "unknown"))

(defn cstring [s]
  (apply str (loop [remaining s acc []]
               (if (seq remaining)
                 (let [this-char (first remaining) this-char-str (first (int-to-ascii this-char))]
                   (if (= 0 this-char)
                     acc
                     (recur (rest remaining) (conj acc this-char-str))))
                 acc))))

(defn not-terminated [s]
  (not (some zero? s)))

(deftype CStringVisualizer
  []
  Visualizer
  (visualize-it [this parsed-section]
    (println (format "%20s: [%s]" (name (:ID parsed-section)) (cstring (:RESULT parsed-section))))
    (let [result (:RESULT parsed-section)]
      (if (seq? result)
        (if (all-zeros result)
          (println (format "%22s%s" "" (str "All " (count result) " bytes are null.")))
          (do (if (not-terminated result)
                (println (format "%22s[%s]" "" "Null terminator not found!")))
            (dump-buffer result)))
        (println result)))
    (println)))

(deftype UDXVisualizer
  []
  Visualizer
  (visualize-it [this parsed-section]
    (let [udx-num (seq-to-num (:RESULT parsed-section))]
      (println (format "%20s: %s [0x%x] [%s]" (name (:ID parsed-section)) udx-num udx-num (get-udx udx-num))))))

; DEFINE FORMATS

(def em-format (binary-protocol (ByteBasedChunker.)
                 (section :ERROR_CODE (FixedLength. 4))
                 (section :ERROR_CLASS (FixedLength. 4))
                 (section :FILENAME (FixedLength. 64))
                 (section :LINE_NUMBER (FixedLength. 4))
                 (section :DESCRIPTION (FixedLength. 128))))

(def am-format (binary-protocol (ByteBasedChunker.)
                 (section :VERSION (FixedLength. 4))
                 (section :SECONDS (FixedLength. 4))
                 (section :MICROSECONDS (FixedLength. 4))
                 (section :SEQUENCE (FixedLength. 4))
                 (section :SUBFUNCTION_CODE (FixedLength. 2))
                 (section :INPUT_DIGEST (FixedLength. 32))
                 (section :OUTPUT_DIGEST (FixedLength. 32))
                 (section :PADDING (FixedLength. 2))
                 (section :ERROR_COUNT (FixedLength. 4))
                 (section :ERROR_FRAME_1 em-format)
                 (section :ERROR_FRAME_2 em-format)
                 (section :ERROR_FRAME_3 em-format)
                 (section :ERROR_FRAME_4 em-format)
                 (section :ERROR_FRAME_5 em-format)
                 (section :ERROR_FRAME_6 em-format)))

; DEFINE VISUALIZERS

(def em-visualizer (BinaryProtocolVisualizer.
                     [(IntegerVisualizer.)
                      (IntegerVisualizer.)
                      (CStringVisualizer.)
                      (IntegerVisualizer.)
                      (CStringVisualizer.)]))

(def visualizers [(IntegerVisualizer.)
                  (DateVisualizer.)
                  (IntegerVisualizer.)
                  (IntegerVisualizer.)
                  (UDXVisualizer.)
                  (HexVisualizer.)
                  (HexVisualizer.)
                  (IntegerVisualizer.)
                  (IntegerVisualizer.)
                  em-visualizer
                  em-visualizer
                  em-visualizer
                  em-visualizer
                  em-visualizer
                  em-visualizer])

; PARSE AND SHOW THE RESULT

;(def test-file "/tmp/valid.bin")
(def test-file "/tmp/invalid.bin")

(deftest parse-binary-file
  (def parsed (parse-binary (FileStreamWrapper. test-file) am-format))
  (is (seq parsed))
  (visualize-binary visualizers parsed))
