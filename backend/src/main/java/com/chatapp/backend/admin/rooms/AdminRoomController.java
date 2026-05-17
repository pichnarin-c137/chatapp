package com.chatapp.backend.admin.rooms;

import com.chatapp.backend.room.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/rooms")
public class AdminRoomController {

    private final AdminRoomService service;

    @GetMapping
    public String list(@RequestParam(value = "page", defaultValue = "0") int page, Model model) {
        Page<ChatRoom> rooms = service.list(page);
        model.addAttribute("pageTitle", "Rooms");
        model.addAttribute("activeNav", "rooms");
        model.addAttribute("rooms", rooms.getContent());
        model.addAttribute("page", page);
        model.addAttribute("totalPages", rooms.getTotalPages());
        return "admin/rooms/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("pageTitle", "New room");
        model.addAttribute("activeNav", "rooms");
        model.addAttribute("room", ChatRoom.builder().type(ChatRoom.Type.GROUP).build());
        model.addAttribute("formAction", "/admin/rooms");
        model.addAttribute("submitLabel", "Create room");
        return "admin/rooms/form";
    }

    @PostMapping
    public String create(@RequestParam String name,
                         @RequestParam(defaultValue = "GROUP") ChatRoom.Type type) {
        service.create(name, type);
        return "redirect:/admin/rooms";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        model.addAttribute("pageTitle", "Edit room");
        model.addAttribute("activeNav", "rooms");
        model.addAttribute("room", service.get(id));
        model.addAttribute("formAction", "/admin/rooms/" + id);
        model.addAttribute("submitLabel", "Save changes");
        return "admin/rooms/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable UUID id,
                         @RequestParam String name,
                         @RequestParam(defaultValue = "GROUP") ChatRoom.Type type) {
        service.update(id, name, type);
        return "redirect:/admin/rooms";
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public String delete(@PathVariable UUID id) {
        service.delete(id);
        return "";
    }
}
