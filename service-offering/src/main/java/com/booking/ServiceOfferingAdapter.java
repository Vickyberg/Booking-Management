package com.booking;

import com.booking.data.exceptions.BookingMgtException;
import com.booking.data.model.Booking;
import com.booking.data.model.BookingStatus;
import com.booking.data.model.SecureUser;
import com.booking.data.model.ServiceOffering;
import com.booking.data.repository.BookingRepo;
import com.booking.data.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceOfferingAdapter {
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final ModelMapper modelMapper;
    private final BusinessService businessService;
    private final BookingRepo bookingRepo;


    public String createServiceOffering(ServiceOfferingRequest serviceOfferingRequest, SecureUser secureUser) throws BookingMgtException {
        ServiceOffering serviceOffering = modelMapper.map(serviceOfferingRequest, ServiceOffering.class);
        log.info("Service Offering: {}", serviceOffering);
        serviceOffering.setBusinessId(businessService.getBusiness(secureUser.getAppUser().getId()).getId());
        log.info("set businness id Service Offering: {}", serviceOffering);
        serviceOfferingRepository.save(serviceOffering);
        return "Service Offering created successfully";
    }

    public Page<ServiceOffering> getAllServiceOffering(int page, int size) {
        return serviceOfferingRepository.findAll(PageRequest.of(page, size));
    }

    public String bookServiceOffering(String serviceId, SecureUser secureUser) throws BookingMgtException {
        ServiceOffering serviceOffering = serviceOfferingRepository.findById(serviceId).orElseThrow(
                () -> new BookingMgtException("Service Offering not found")
        );
        Booking booking = new Booking();
        booking.setUserEmail(secureUser.getAppUser().getEmail());
        booking.setBookingStatus(BookingStatus.PENDING);
        bookingRepo.saveAndFlush(booking);
        serviceOffering.getBookings().add(booking);
        serviceOfferingRepository.save(serviceOffering);
        return "Service Offering booked successfully";
    }

    public String acceptBooking(String serviceId, String userEmail) {
        toggleStatus(serviceId, userEmail, BookingStatus.ACCEPTED);
        return "Booking accepted successfully";
    }

    private void toggleStatus(String serviceId, String userEmail, BookingStatus status) {
        ServiceOffering serviceOffering = serviceOfferingRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service Offering not found"));
        List<Booking> bookings = serviceOffering.getBookings();
        Booking booking = bookings.stream().filter(booking1 -> booking1.getUserEmail()
                .equals(userEmail)).findFirst().orElseThrow();
        bookings.remove(booking);
        booking.setBookingStatus(status);
        booking.setServiceOffering(serviceOffering);
        bookingRepo.saveAndFlush(booking);
        bookings.add(booking);
        serviceOffering.setBookings(bookings);
        serviceOfferingRepository.save(serviceOffering);
    }

    public String cancelBooking(String serviceId, String userEmail) {
        toggleStatus(serviceId, userEmail, BookingStatus.REJECTED);
        return "Booking canceled successfully";
    }

    public Page<Booking> getAllServiceOfferingByUserEmail(String userEmail, int page, int size) {

        return bookingRepo.findAllByUserEmail(userEmail, PageRequest.of(page, size));

    }
}
