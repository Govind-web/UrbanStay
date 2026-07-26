package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.entity.Room;

public interface InventoryService {
    void initializeRoomForYear(Room room);

    void deleteFutureInventories(Room room);

}
