CREATE TABLE scheduled_alerts (
  id varchar(36) PRIMARY KEY,
  type varchar(10) NOT NULL CHECK (type IN ('PUSH', 'MODAL')),
  title varchar(120) NOT NULL,
  body varchar(1000) NOT NULL,
  url varchar(255),
  scheduled_at varchar(255) NOT NULL,
  created_at varchar(255) NOT NULL,
  dispatched_at varchar(255)
);
CREATE INDEX scheduled_alerts_due_idx ON scheduled_alerts (scheduled_at) WHERE dispatched_at IS NULL;

CREATE TABLE seen_alerts (
  user_id varchar(255) NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
  alert_id varchar(36) NOT NULL REFERENCES scheduled_alerts(id) ON DELETE CASCADE,
  seen_at varchar(255) NOT NULL,
  PRIMARY KEY (user_id, alert_id)
);
