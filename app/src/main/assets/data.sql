INSERT INTO manufacturer (short_name, long_name)
    VALUES ('HTC', 'HTC Corporation');

INSERT INTO manufacturer (short_name, long_name)
    VALUES ('Samsung', 'Samsung Electronics');

INSERT INTO manufacturer (short_name, long_name)
    VALUES ('LG', 'LG Electronics');

INSERT INTO device (model, nickname, display_size_inches, manufacturer_id)
    VALUES ('Nexus One', 'Passion', 3.7, (SELECT _id
                                          FROM manufacturer
                                          WHERE short_name = 'HTC'));

INSERT INTO device (model, nickname, display_size_inches, manufacturer_id)
    VALUES ('Nexus S', 'Crespo', 4.0, (SELECT _id
                                       FROM manufacturer
                                       WHERE short_name = 'Samsung'));

INSERT INTO device (model, nickname, display_size_inches, manufacturer_id)
    VALUES ('Galaxy Nexus', 'Toro', 4.65, (SELECT _id
                                           FROM manufacturer
                                           WHERE short_name = 'Samsung'));

INSERT INTO device (model, nickname, display_size_inches, manufacturer_id)
    VALUES ('Nexus 4', 'Mako', 4.7, (SELECT _id
                                     FROM manufacturer
                                     WHERE short_name = 'LG'));