package ru.transferservis.driver.data.model;

import java.util.List;

public final class DriverAssignmentsResponse {

    private boolean ok;

    private int count;

    private List<DriverAssignmentDto> assignments;

    public boolean isOk() {

        return ok;
    }

    public int getCount() {

        return count;
    }

    public List<DriverAssignmentDto> getAssignments() {

        return assignments;
    }
}
