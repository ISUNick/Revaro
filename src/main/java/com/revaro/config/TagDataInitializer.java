package com.revaro.config;

import com.revaro.entity.Tag;
import com.revaro.enums.TagCategory;
import com.revaro.repository.TagRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TagDataInitializer implements ApplicationRunner {

    private final TagRepository tagRepository;

    public TagDataInitializer(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (tagRepository.count() > 0) return;

        tagRepository.saveAll(List.of(
            new Tag("JDM", TagCategory.STYLE),
            new Tag("Euro", TagCategory.STYLE),
            new Tag("American Muscle", TagCategory.STYLE),
            new Tag("Lowrider", TagCategory.STYLE),
            new Tag("Stance", TagCategory.STYLE),
            new Tag("Classic", TagCategory.STYLE),
            new Tag("Rat Rod", TagCategory.STYLE),
            new Tag("Off-Road / 4x4", TagCategory.STYLE),
            new Tag("Truck", TagCategory.STYLE),
            new Tag("Motorcycle", TagCategory.STYLE),
            new Tag("Cars and Coffee", TagCategory.VIBE),
            new Tag("Cruise Night", TagCategory.VIBE),
            new Tag("Meet and Greet", TagCategory.VIBE),
            new Tag("Pop-Up", TagCategory.VIBE),
            new Tag("Charity Event", TagCategory.VIBE),
            new Tag("Weekly Meet", TagCategory.VIBE),
            new Tag("Time Attack", TagCategory.PERFORMANCE),
            new Tag("Drag Racing", TagCategory.PERFORMANCE),
            new Tag("Autocross", TagCategory.PERFORMANCE),
            new Tag("Drift", TagCategory.PERFORMANCE),
            new Tag("Rally", TagCategory.PERFORMANCE),
            new Tag("Road Course", TagCategory.PERFORMANCE),
            new Tag("Dyno Day", TagCategory.PERFORMANCE),
            new Tag("Roll Racing", TagCategory.PERFORMANCE),
            new Tag("Honda", TagCategory.BRANDS),
            new Tag("Toyota", TagCategory.BRANDS),
            new Tag("Subaru", TagCategory.BRANDS),
            new Tag("BMW", TagCategory.BRANDS),
            new Tag("Mercedes", TagCategory.BRANDS),
            new Tag("Porsche", TagCategory.BRANDS),
            new Tag("Ford", TagCategory.BRANDS),
            new Tag("Chevy", TagCategory.BRANDS),
            new Tag("Dodge", TagCategory.BRANDS),
            new Tag("Nissan", TagCategory.BRANDS),
            new Tag("Mitsubishi", TagCategory.BRANDS),
            new Tag("Supercar", TagCategory.BRANDS),
            new Tag("Jeep", TagCategory.BRANDS),
            new Tag("Family Friendly", TagCategory.OTHER),
            new Tag("Free Entry", TagCategory.OTHER),
            new Tag("Paid Entry", TagCategory.OTHER),
            new Tag("Vendor Booths", TagCategory.OTHER),
            new Tag("Food Trucks", TagCategory.OTHER),
            new Tag("All Are Welcome", TagCategory.OTHER),
            new Tag("Invite Only", TagCategory.OTHER)
        ));
    }
}
