/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import org.springframework.samples.petclinic.owner.OwnerNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Wick Dynex
 */
@Controller
@RequestMapping("/owners/{ownerId}")
class PetController {

    private static final String VIEWS_PETS_CREATE_OR_UPDATE_FORM = "pets/createOrUpdatePetForm";

    private final OwnerRepository owners;

    public PetController(OwnerRepository owners) {
        this.owners = owners;
    }

    @ModelAttribute("types")
    public Collection<PetType> populatePetTypes() {
        return this.owners.findPetTypes();
    }

    @ModelAttribute("owner")
    public Owner findOwner(@PathVariable("ownerId") int ownerId) {
        return this.owners.findById(ownerId).orElseThrow(() -> new OwnerNotFoundException(
                "Owner not found with id: " + ownerId));
    }

    @ModelAttribute("pet")
    public Pet findPet(@PathVariable("ownerId") int ownerId,
            @PathVariable(name = "petId", required = false) Integer petId) {
        Owner owner = this.owners.findById(ownerId).orElseThrow(() -> new OwnerNotFoundException(
                "Owner not found with id: " + ownerId));
        if (petId == null) {
            return new Pet();
        }
        return owner.getPet(petId);
    }

    @InitBinder("owner")
    public void initOwnerBinder(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields("id");
    }

    @InitBinder("pet")
    public void initPetBinder(WebDataBinder dataBinder) {
        dataBinder.setValidator(new PetValidator());
    }

    @GetMapping("/pets/new")
    public String initCreationForm(Owner owner, Model model) {
        Pet pet = new Pet();
        model.addAttribute("pet", pet);
        return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
    }

    @PostMapping("/pets/new")
    public String processCreationForm(Owner owner, @Valid Pet pet, BindingResult result, Model model) {
        if (StringUtils.hasText(pet.getName()) && owner.getPet(pet.getName(), true) != null) {
            result.rejectValue("name", "duplicatePetName", "This pet name already exists for this owner.");
        }
        LocalDate currentDate = LocalDate.now();
        if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
            result.rejectValue("birthDate", "invalidBirthDate", "Birth date cannot be in the future.");
        }
        if (result.hasErrors()) {
            model.addAttribute("pet", pet); // Re-add pet to model to preserve entered data
            return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
        }
        owner.addPet(pet);
        this.owners.save(owner);
        model.addAttribute("message", "New Pet has been Added"); // Add success message to the model
        return "redirect:/owners/" + owner.getId();
    }

    @GetMapping("/pets/{petId}/edit")
    public String initUpdateForm(@PathVariable("petId") int petId, Owner owner, Model model) {
        Pet pet = owner.getPet(petId);
        if (pet == null) {
            throw new PetNotFoundException("Pet not found with id: " + petId);
        }
        model.addAttribute("pet", pet);
        return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
    }

    @PostMapping("/pets/{petId}/edit")
    public String processUpdateForm(Owner owner, @Valid Pet pet, BindingResult result, Model model) {
        if (StringUtils.hasText(pet.getName())) {
            Pet existingPet = owner.getPet(pet.getName(), false);
            if (existingPet != null && !existingPet.getId().equals(pet.getId())) {
                result.rejectValue("name", "duplicatePetName", "This pet name already exists for this owner.");
            }
        }
        LocalDate currentDate = LocalDate.now();
        if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
            result.rejectValue("birthDate", "invalidBirthDate", "Birth date cannot be in the future.");
        }
        if (result.hasErrors()) {
            model.addAttribute("pet", pet); // Re-add pet to model to preserve entered data
            return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
        }
        updateExistingPet(pet);
        this.owners.save(owner);
        model.addAttribute("message", "Pet details has been edited"); // Add success message to the model
        return "redirect:/owners/" + owner.getId();
    }

    private void updateExistingPet(Pet pet) {
        Optional<Pet> existingPetOptional = this.owners.findPetById(pet.getId());
        if (existingPetOptional.isPresent()) {
            Pet existingPet = existingPetOptional.get();
            existingPet.setName(pet.getName());
            existingPet.setBirthDate(pet.getBirthDate());
            existingPet.setType(pet.getType());
            this.owners.save(existingPet);
        } else {
            throw new PetNotFoundException("Pet not found with id: " + pet.getId());
        }
    }

    private void addNewPet(Owner owner, Pet pet) {
        owner.addPet(pet);
        this.owners.save(owner);
    }

}
