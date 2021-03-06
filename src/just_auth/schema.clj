;; Just auth - a simple two factor authenticatiokn library

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2017 Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti  <aspra@dyne.org>

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns just-auth.schema
  (:require [clj-storage.core :refer [Store]]
            [schema.core :as s]))

(def StoreSchema clj_storage.core.Store)

(s/defschema HashFns
  {:hash-fn clojure.lang.Fn
   :hash-check-fn clojure.lang.Fn})

(s/defschema AuthStores
  {(s/required-key "account") StoreSchema
   (s/required-key "passwordrecovery") StoreSchema
   (s/required-key "failedlogin") StoreSchema})

(def EmailSignUp
  {:name s/Str
   :othernames [s/Str]
   :email s/Str ;;TODO email reg exp?
   :password s/Str
   :activationuri s/Str ;; TODO URI
   })

(def ThrottlingConfig
  {:criteria #{(s/maybe (s/enum :email :ipaddress))} 
   :type (s/enum :block :delay)
   :time-window-secs s/Num
   :threshold s/Num})

(def EmailConfig
  {:email-server s/Str 
   :email-user s/Str
   :email-pass s/Str
   :email-address s/Str
   (s/optional-key :email-admin) s/Str})

(def StubEmailConfig
  {(s/optional-key :email-admin) s/Str})
