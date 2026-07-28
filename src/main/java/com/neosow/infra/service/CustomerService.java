package com.neosow.infra.service;

import com.neosow.infra.dto.customer.CustomerDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface CustomerService {
    CustomerDTO createCustomer(CustomerDTO customerDto);
    CustomerDTO getCustomerById(UUID id);
    CustomerDTO updateCustomer(UUID id, CustomerDTO customerDto);
    void deleteCustomer(UUID id);
    Page<CustomerDTO> getCustomers(String query, int page, int size);
}
