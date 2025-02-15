(ns cljm.render
  (:require
   [babashka.fs :as fs]
   [clojure.string :as string]
   [clojure.tools.reader :as reader]
   [hickory.core :as hc]
   [hickory.zip :as hz]
   [clojure.zip :as zip]
   [hiccup2.core :as h2]))

(defn read-file [path]
  (let [file (fs/file path)]
    (if (fs/exists? file)
      (slurp file)
      (throw (Exception. (str "File " path " does not exist"))))))

(defn write-html [path html]
  (spit (fs/file path) html))

(defn path-to-ns-file
  "Returns the path to the namespace file for the given namespace.
   Throws an exception if the namespace file does not exist."
  [ns]
  (let [path (-> ns
                 ns-publics 
                 vec first second 
                 meta :file)] 
    (if (nil? path)
      (throw (Exception. "No vars found in namespace. Cannot deduce namespace file"))
      path)))

(defn find-template 
  "Returns HTML template for the given namespace.
   Throws an exception if the namespace file does not exist."
  [ns]
  (let [path (path-to-ns-file ns)]
    (read-file (string/replace path #"\.clj$" ".html"))))

(defn substitute-vars
  "Substitute {{var}} or {{code}} in the HTML template with their evaluated values.
   Returns HTML string with substituted values."
  [html-template]
  (string/replace html-template #"\{\{([^}]+)\}\}"
                  (fn [[_ code]]
                    (let [result (eval (reader/read-string code))]
                      (str result)))))

;; (defn write-html-file []
;;   (let [html (fill-template)]
;;     (let [path (path-to-html-file)]
;;       (write-html path html))))

(defn- edit-nodes [condition edit-fn z]
  (loop [loc z]
    (if (zip/end? loc)
      (zip/root loc)
      (let [node (zip/node loc)]
        (if (condition node)
          (recur (zip/edit loc edit-fn))
          (recur (zip/next loc)))))))

(defn- get-comp-fn
  "Returns the component function for the given tag.
   Returns nil if the tag is not a component."
  [tag]
  (let [tag-name (name tag) 
        tag-parts (string/split tag-name #":")]
    (if (= (count tag-parts) 2)
      (let [tag-ns (first tag-parts)
            tag-name (second tag-parts)
            comp-fn (requiring-resolve (symbol tag-ns tag-name))]
        comp-fn)
      nil)))

(defn- update-node
  "Updates the node with the component function.
   Returns the updated node."
  [node & _] (let [comp-fn (get-comp-fn (first node))
                   attrs (second node)]
               (if (map? attrs)
                 (let [body (or (nnext node) '())]
                   (apply comp-fn (cons attrs body)))
                 (let [body (or (nnext node) '())]
                   (apply comp-fn (cons {} body))))))

(defn- should-edit-node?
  "Returns true if the node should be edited.
   Returns false otherwise."
  [node & _]
  (if (vector? node)
    (let [tag (first node)
          comp-fn (get-comp-fn tag)]
      (some? comp-fn))
    false))

(defn resolve-comps
  "Takes HTML string, resolves and renders the components.
   Returns hiccup structure."
  [html]
  (let [hiccup (hc/as-hiccup (hc/parse html))
        zipper (hz/hiccup-zip hiccup)
        edited (edit-nodes should-edit-node?
                           update-node
                           zipper)]
     edited))

(defn render-html
  "Takes hiccup structure and renders it to HTML string."
  [hiccup & {:keys [escape-strings?] :or {escape-strings? false}}]
  (h2/html {:escape-strings? escape-strings?} hiccup))

(defn page []
  (-> *ns*
      find-template
      substitute-vars
      resolve-comps
      render-html
      str))
