CREATE TABLE transfer_status_history (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by BIGINT,
    note NVARCHAR(500),
    created_at DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_transfer_history_transfer FOREIGN KEY (transfer_id) REFERENCES transfers(id),
    CONSTRAINT fk_transfer_history_actor FOREIGN KEY (changed_by) REFERENCES users(id)
);
CREATE INDEX ix_transfer_history_time ON transfer_status_history(transfer_id, created_at, id);

CREATE TABLE employee_assignment_history (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    changed_by BIGINT,
    previous_branch_id BIGINT,
    new_branch_id BIGINT,
    previous_role VARCHAR(20),
    new_role VARCHAR(20) NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    note NVARCHAR(500),
    created_at DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_employee_history_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_employee_history_actor FOREIGN KEY (changed_by) REFERENCES users(id),
    CONSTRAINT fk_employee_history_previous_branch FOREIGN KEY (previous_branch_id) REFERENCES branches(id),
    CONSTRAINT fk_employee_history_new_branch FOREIGN KEY (new_branch_id) REFERENCES branches(id)
);
CREATE INDEX ix_employee_history_time ON employee_assignment_history(user_id, created_at, id);

