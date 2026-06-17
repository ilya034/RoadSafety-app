Enum risk_level {
  green   // Safe area / city block
  yellow  // Potential risk / crossing
  red     // Dangerous area / road
}

Table users {
  Id uuid [pk]
  hashed_password varchar(255) [not null]
  first_name varchar(100)
  last_name varchar(100)
  patronymic varchar(100)
  birth_date date
  mail_address citext [unique]
  phone_number varchar(40) [unique]
  created_at timestamp [not null, default: "now()"]
}

Table families {
  Id uuid [pk]
  created_by_user_id uuid [not null]
  name text
  city_id varchar [note: "Assigned city for safety zones"]
  created_at timestamp [not null, default: "now()"]
}

Table family_members {
  FamilyId uuid [not null]
  UserId uuid [not null]
  Role int [not null, note: "FamilyMemberRole enum value"]
  joined_at timestamp [not null, default: "now()"]

  indexes {
    (FamilyId, UserId) [pk]
    UserId
  }
}

Table invite_codes {
  Id uuid [pk]
  value varchar(6) [not null, unique, note: "Base58 invite code"]
  role citext [not null, note: "FamilyMemberRole string: Parent or Child"]
  family_id uuid [not null]
  created_by_user_id uuid [not null]
  expires_at timestamp [not null]
  accepted_at timestamp
  is_used bool [not null]
  created_at timestamp [not null, default: "now()"]

  indexes {
    value [unique]
    family_id
    created_by_user_id
  }
}

Table sessions {
  Id uuid [pk]
  UserId uuid [not null]
  is_revoked bool [not null]
  created_at timestamp [not null, default: "now()"]

  indexes {
    UserId
  }
}

Table refresh_tokens {
  Id uuid [pk]
  token_hash varchar [not null]
  UserId uuid [not null]
  expires_at timestamp [not null]
  is_used bool [not null]
  is_revoked bool [not null]
  replaced_by_token_id uuid
  created_at timestamp [not null, default: "now()"]
  session_id uuid [not null]

  indexes {
    UserId
    session_id
    replaced_by_token_id
  }
}

Table map_areas {
  id uuid [pk]
  osm_id bigint [note: "Nullable for generated green zones"]
  base_area_key varchar [not null, unique, note: "Stable base polygon key used by family overrides"]
  risk risk_level [not null]
  geom geometry(Polygon, 4326) [not null, note: "PostGIS polygon in WGS84"]
  city_id varchar [note: "Batch update key"]

  indexes {
    geom [type: gist, name: "map_areas_geom_idx"]
    city_id
    base_area_key [unique]
    risk
  }
}

Table user_map_areas {
  id uuid [pk]
  family_id uuid [not null]
  child_id uuid [note: "Null means the area applies to all children in the family"]
  base_area_key varchar [note: "Set for risk-only override of a generated base area; null for custom polygons"]
  risk risk_level [not null]
  geom geometry(Polygon, 4326) [note: "Set for custom polygons; null for base-area overrides"]
  created_by_user_id uuid [not null]
  created_at timestamp [not null, default: "now()"]
  updated_at timestamp [not null, default: "now()"]

  indexes {
    geom [type: gist, name: "user_areas_geom_idx"]
    (family_id, child_id)
    base_area_key
  }
}

Table child_locations {
  child_id uuid [pk]
  geom geometry(Point, 4326) [not null]
  accuracy_meters double
  last_updated_at timestamp [not null]
  current_risk risk_level [not null]
}

Table child_stats {
  child_id uuid [pk]
  total_score int [not null, default: 0]
  rating int [not null, default: 0]
}

Table location_history {
  id uuid [pk]
  child_id uuid [not null]
  date date [not null]
  route geometry(LineString, 4326) [not null]

  indexes {
    (child_id, date)
  }
}

Ref: family_members.FamilyId > families.Id [delete: cascade]
Ref: family_members.UserId > users.Id [delete: cascade]

Ref: invite_codes.family_id > families.Id [delete: cascade]
Ref: invite_codes.created_by_user_id > users.Id [delete: restrict]

Ref: sessions.UserId > users.Id [delete: cascade]

Ref: refresh_tokens.session_id > sessions.Id [delete: cascade]
Ref: refresh_tokens.UserId > users.Id [delete: cascade]
Ref: refresh_tokens.replaced_by_token_id > refresh_tokens.Id [delete: restrict]

Ref: user_map_areas.family_id > families.Id [delete: cascade]
Ref: user_map_areas.child_id > users.Id [delete: cascade]
Ref: user_map_areas.created_by_user_id > users.Id [delete: restrict]

Ref: child_locations.child_id > users.Id [delete: cascade]
Ref: child_stats.child_id > users.Id [delete: cascade]
Ref: location_history.child_id > users.Id [delete: cascade]
