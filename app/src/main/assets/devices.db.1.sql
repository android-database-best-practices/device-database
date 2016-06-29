-- device table
CREATE TABLE device (_id INTEGER PRIMARY KEY AUTOINCREMENT,
                     model TEXT NOT NULL,
                     nickname TEXT,
                     memory_mb REAL,
                     display_size_inches REAL,
                     manufacturer_id INTEGER REFERENCES manufacturer(_id) ON DELETE CASCADE);
CREATE INDEX idx_device_model ON device(model);

-- manufacturer table
CREATE TABLE manufacturer(_id INTEGER PRIMARY KEY AUTOINCREMENT,
                          short_name TEXT,
                          long_name);