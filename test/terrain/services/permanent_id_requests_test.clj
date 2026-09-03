(ns terrain.services.permanent-id-requests-test
  (:require [clojure.test :refer [deftest testing use-fixtures are is]]
            [slingshot.test]
            [terrain.services.permanent-id-requests :as pir]
            [terrain.test-fixtures :refer [with-test-config]]
            [terrain.util.config :as config]))

(use-fixtures :once with-test-config)

(def ^:private ckan-dataset-name #'pir/ckan-dataset-name)
(def ^:private format-metadata-target-url #'pir/format-metadata-target-url)

(defn- title-avus
  [& titles]
  (mapv (fn [title] {:attr "title" :value title}) titles))

(deftest ckan-dataset-name-matches-the-sync-rule
  (testing "title-to-name pairs taken verbatim from the live catalog"
    (are [title expected] (= expected (ckan-dataset-name title))
      "Genomes to Fields 2023 dataset"
      "genomes_to_fields_2023_dataset"

      ;; commas and colons are dropped outright, not replaced with a separator
      "Protein Misfolding, Proteostasis, and Aging"
      "protein_misfolding_proteostasis_and_aging"

      "Oceans of Disorder: Elucidating the Role of Disordered Proteins in Cellular Adaptation"
      "oceans_of_disorder_elucidating_the_role_of_disordered_proteins_in_cellular_adaptation"

      ;; parentheses and periods likewise vanish, closing up the surrounding text
      "Tx303 Whole Genome Bisulfate Sequencing (WGBS)"
      "tx303_whole_genome_bisulfate_sequencing_wgbs"

      "U.Nottm_2016_root_images"
      "unottm_2016_root_images"

      ;; hyphens and underscores are the only punctuation that survives
      "Two-year EMS-mutagenized sorghum field scanner dataset"
      "two-year_ems-mutagenized_sorghum_field_scanner_dataset"))

  (testing "spaces become underscores before filtering, so a run of spaces yields a run of them"
    (is (= "a__b" (ckan-dataset-name "a  b")))
    ;; a character dropped from between two spaces leaves both underscores behind, rather than
    ;; collapsing them into one separator
    (is (= "a__b" (ckan-dataset-name "a . b"))))

  (testing "titles are lower-cased"
    (is (= "mixedcase" (ckan-dataset-name "MixedCASE"))))

  (testing "a title longer than CKAN's 100-character limit truncates to exactly 100"
    (let [title "Data-Driven Discovery of Regulatory Mechanisms and Cellular Resource Allocation via Multi-Modal Data Integration"
          name  (ckan-dataset-name title)]
      (is (= 100 (count name)))
      (is (= "data-driven_discovery_of_regulatory_mechanisms_and_cellular_resource_allocation_via_multi-modal_data"
             name))))

  (testing "a name shorter than CKAN's 2-character minimum is rejected"
    ;; pure punctuation slugifies to the empty string
    (is (thrown+? [:type :clojure-commons.exception/bad-request] (ckan-dataset-name "...")))
    (is (thrown+? [:type :clojure-commons.exception/bad-request] (ckan-dataset-name "x")))
    (is (thrown+? [:type :clojure-commons.exception/bad-request] (ckan-dataset-name nil)))
    ;; two characters is acceptable
    (is (= "ab" (ckan-dataset-name "ab")))))

(deftest ckan-dataset-name-is-locale-independent
  (testing "an uppercase I lower-cases to ASCII i regardless of the JVM's default locale"
    (let [default (java.util.Locale/getDefault)]
      (try
        (java.util.Locale/setDefault (java.util.Locale. "tr" "TR"))
        (is (= "istanbul_images" (ckan-dataset-name "ISTANBUL Images")))
        (finally
          (java.util.Locale/setDefault default))))))

(deftest target-url-points-at-the-ckan-dataset-page
  (testing "the URL is the configured base plus the dataset name"
    (is (= "http://perm-id-target-base-url/dataset/genomes_to_fields_2023_dataset"
           (format-metadata-target-url (title-avus "Genomes to Fields 2023 dataset")))))

  (testing "a trailing slash on the base does not produce a doubled slash"
    (with-redefs [config/permanent-id-target-base-url (constantly "https://dc.cyverse.org/dataset/")]
      (is (= "https://dc.cyverse.org/dataset/some_dataset"
             (format-metadata-target-url (title-avus "Some Dataset"))))))

  (testing "a base without a trailing slash gets one"
    (with-redefs [config/permanent-id-target-base-url (constantly "https://dc.cyverse.org/dataset")]
      (is (= "https://dc.cyverse.org/dataset/some_dataset"
             (format-metadata-target-url (title-avus "Some Dataset"))))))

  (testing "the first title AVU wins, matching the primary title DataCite records"
    (is (= "http://perm-id-target-base-url/dataset/first_title"
           (format-metadata-target-url (title-avus "First Title" "Second Title")))))

  (testing "unrelated AVUs are ignored"
    (is (= "http://perm-id-target-base-url/dataset/the_title"
           (format-metadata-target-url [{:attr "creator" :value "Somebody"}
                                        {:attr "title" :value "The Title"}
                                        {:attr "publisher" :value "CyVerse"}])))))
