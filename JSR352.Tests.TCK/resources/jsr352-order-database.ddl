CONNECT 'jdbc:derby:ORDERDB;create=true';

DROP TABLE Orders;
DROP TABLE Inventory;

CREATE TABLE Orders (
  id		BIGINT,
  itemID	INT,
  quantity  INT
);

CREATE TABLE Inventory(
  itemID	INT NOT NULL PRIMARY KEY,
  quantity	INT NOT NULL
);

INSERT INTO Inventory
VALUES (1, 100);
