package com.smalaca.rentalapplication.domain.booking;

import com.smalaca.rentalapplication.domain.period.Period;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.smalaca.rentalapplication.domain.booking.BookingStatus.ACCEPTED;
import static com.smalaca.rentalapplication.domain.booking.BookingStatus.REJECTED;

@Entity
@SuppressWarnings("PMD.UnusedPrivateField")
public class Booking {
    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    private RentalType rentalType;
    private String rentalPlaceId;
    private String tenantId;
    @ElementCollection
    private List<LocalDate> days;
    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus = BookingStatus.OPEN;

    private Booking() {}

    private Booking(RentalType rentalType, String rentalPlaceId, String tenantId, List<LocalDate> days) {
        this.rentalType = rentalType;
        this.rentalPlaceId = rentalPlaceId;
        this.tenantId = tenantId;
        this.days = days;
    }

    public static Booking apartment(String rentalPlaceId, String tenantId, Period period) {
        List<LocalDate> days = period.asDays();

        return new Booking(RentalType.APARTMENT, rentalPlaceId, tenantId, days);
    }

    public static Booking hotelRoom(String rentalPlaceId, String tenantId, List<LocalDate> days) {
        return new Booking(RentalType.HOTEL_ROOM, rentalPlaceId, tenantId, days);
    }

    public void reject(BookingEventsPublisher bookingEventsPublisher) {
        bookingStatus = bookingStatus.moveTo(REJECTED);

        bookingEventsPublisher.bookingRejected(rentalType, rentalPlaceId, tenantId, days);
    }

    public void accept(BookingEventsPublisher bookingEventsPublisher) {
        bookingStatus = bookingStatus.moveTo(ACCEPTED);

        bookingEventsPublisher.bookingAccepted(rentalType, rentalPlaceId, tenantId, days);
    }

    public String id() {
        return id.toString();
    }

    boolean hasCollisionWith(Booking booking) {
        return bookingStatus.equals(ACCEPTED) && hasDaysCollisionWith(booking);
    }

    private boolean hasDaysCollisionWith(Booking booking) {
        return days.stream().anyMatch(day -> booking.days.contains(day));
    }

    public RentalPlaceIdentifier rentalPlaceIdentifier() {
        return new RentalPlaceIdentifier(rentalType, rentalPlaceId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Booking booking = (Booking) o;

        if (!days.containsAll(booking.days)) {
            return false;
        }

        return new EqualsBuilder()
                .append(rentalType, booking.rentalType)
                .append(rentalPlaceId, booking.rentalPlaceId)
                .append(tenantId, booking.tenantId)
                .isEquals();
    }

    @Override
    @SuppressWarnings("checkstyle:MagicNumber")
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(rentalType)
                .append(rentalPlaceId)
                .append(tenantId)
                .append(days)
                .toHashCode();
    }
}
