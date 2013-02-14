CONNECT 'jdbc:derby:ORDERDB;create=true';

DROP TABLE Orders;
DROP TABLE Inventory;

CREATE TABLE Orders (
  orderID	INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY,
  itemID	INT,
  quantity  INT
);

CREATE TABLE Inventory(
  itemID	INT NOT NULL PRIMARY KEY,
  quantity	INT NOT NULL
);

INSERT INTO Inventory VALUES (1, 100);
