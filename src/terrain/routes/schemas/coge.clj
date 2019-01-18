(ns terrain.routes.schemas.coge
  (:use [common-swagger-api.schema :only [describe]]
        [schema.core :only [defschema maybe]]))

(defschema GenomeSearchParams
  {:search (describe String "The genome search string, which must be at least three characters long.")})

(defschema Organism
  {:description (describe (maybe String) "The organism description.")
   :id          (describe Long "The organism ID.")
   :name        (describe (maybe String) "The organism name.")})

(defschema SequenceType
  {:description (describe (maybe String) "The sequence type description.")
   :id          (describe String "The sequence type ID.")
   :name        (describe (maybe String) "The sequence type name.")})

(defschema Genome
  {:description      (describe (maybe String) "The genome description.")
   :deleted          (describe (maybe Boolean) "True if the genome has been marked as deleted.")
   :organism         (describe Organism "Information about the organism associated with the genome.")
   :organism_id      (describe Long "The organism ID.")
   :name             (describe (maybe String) "The genome name.")
   :chromosome_count (describe (maybe Long) "The number of chromosomes in the genome.")
   :restricted       (describe Boolean "True if access to the genome is restricted.")
   :link             (describe (maybe String) "The link to the genome information if available.")
   :id               (describe Long "The genome ID.")
   :certified        (describe Boolean "True if the genome is certified.")
   :info             (describe (maybe String) "General information about the genome.")
   :version          (describe (maybe String) "The genome version.")
   :sequence_type    (describe SequenceType "The sequence type information.")})

(defschema GenomeSearchResponse
  {:genomes (describe [Genome] "The list of genomes.")})
