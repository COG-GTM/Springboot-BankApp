package com.example.exampleapi.service;

import com.example.exampleapi.model.Item;
import com.example.exampleapi.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public Optional<Item> getItemById(Long id) {
        return itemRepository.findById(id);
    }

    public Item createItem(Item item) {
        item.setCreatedAt(LocalDateTime.now());
        return itemRepository.save(item);
    }

    public Optional<Item> updateItem(Long id, Item item) {
        return itemRepository.findById(id).map(existingItem -> {
            existingItem.setName(item.getName());
            existingItem.setDescription(item.getDescription());
            return itemRepository.save(existingItem);
        });
    }

    public boolean deleteItem(Long id) {
        if (itemRepository.existsById(id)) {
            itemRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
