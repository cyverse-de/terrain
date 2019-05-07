(ns terrain.routes.schemas.coge
  (:use [common-swagger-api.schema :only [describe]]
        [schema.core :only [defschema maybe optional-key]]))

(def GenomeIdPathParam (describe Long "The genome ID"))

(defschema GenomeSearchParams
  {:search (describe String "The genome search string, which must be at least three characters long")})

(defschema GenomeExportParams
  {(optional-key :notify)
   (describe Boolean "Set to `true` to be notified when the export is complete (defaults to `false`)")

   (optional-key :overwrite)
   (describe Boolean (str "Set to `true` to indicate that the file should be overwritten if it exists "
                          "(defaults to `false`)"))})

(defschema GenomeLoadRequest
  {:paths (describe [String] "The paths to the files in the data store to view in CoGe.")})

(defschema Organism
  {:description (describe (maybe String) "The organism description")
   :id          (describe Long "The organism ID")
   :name        (describe (maybe String) "The organism name")})

(defschema SequenceType
  {:description (describe (maybe String) "The sequence type description")
   :id          (describe String "The sequence type ID")
   :name        (describe (maybe String) "The type of genomic sequence (e.g. \"masked\", \"unmasked\")")})

(defschema Genome
  {:description      (describe (maybe String) "The genome description")
   :deleted          (describe (maybe Boolean) "True if the genome has been marked as deleted")
   :organism         (describe Organism "Information about the organism associated with the genome")
   :organism_id      (describe Long "The organism ID")
   :name             (describe (maybe String) "The genome name")
   :chromosome_count (describe (maybe Long) "The number of chromosomes in the genome")
   :restricted       (describe Boolean "True if access to the genome is restricted")
   :link             (describe (maybe String) "Optional user-supplied link to the genome information source")
   :id               (describe Long "The genome ID")
   :certified        (describe Boolean "True if the genome is certified")
   :info             (describe (maybe String) "General information about the genome")
   :version          (describe (maybe String) "The genome version")
   :sequence_type    (describe SequenceType "The type of genomic sequence (e.g. \"masked\", \"unmasked\")")})

(defschema GenomeSearchResponse
  {:genomes (describe [Genome] "The list of genomes")})

(defschema GenomeExportResponse
  {:id      (describe Long "The genome ID")
   :success (describe Boolean "True if the genome export request was successfully submitted")})

(defschema GenomeLoadResponse
  {:coge_genome_url (describe String "The URL to use when viewing the files being imported into CoGe")})
