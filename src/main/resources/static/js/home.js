// Homepage: filter panel, sort menu, clickable cards, and distance based sorting
(function () {
    'use strict';

    const MAX_DISTANCE = 150;
    const NO_LOCATION_MILES = 999;
    const PAST_EVENT_SCORE = 1e9;

    const form = document.getElementById('searchForm');
    const filterPanel = document.getElementById('filterPanel');
    const sortInput = document.getElementById('sortInput');
    const feed = document.getElementById('eventFeed');
    const cards = feed ? Array.from(feed.querySelectorAll('.event-card')) : [];
    let userLocation = null;

    document.getElementById('filterToggle').addEventListener('click', function () {
        filterPanel.hidden = !filterPanel.hidden;
    });

    form.querySelectorAll('input[name="searchIn"]').forEach(function (radio) {
        radio.addEventListener('change', function () {
            form.submit();
        });
    });

    form.querySelectorAll('[data-sort]').forEach(function (button) {
        button.addEventListener('click', function () {
            sortInput.value = button.dataset.sort;
            form.submit();
        });
    });

    // The whole card opens the event, except for links inside it
    cards.forEach(function (card) {
        card.addEventListener('click', function (e) {
            if (!e.target.closest('a, button')) {
                window.location.href = card.dataset.url;
            }
        });
    });

    const slider = document.getElementById('distanceSlider');
    const distanceLabel = document.getElementById('distanceLabel');
    slider.addEventListener('input', function () {
        const miles = Number(slider.value);
        distanceLabel.textContent = miles >= MAX_DISTANCE ? 'Any' : miles + ' mi';
        if (userLocation) {
            cards.forEach(function (card) {
                const distance = distanceTo(card);
                card.hidden = miles < MAX_DISTANCE && distance !== null && distance > miles;
            });
        }
    });

    // Use the last known location right away, then refresh it in the background
    const cachedLat = parseFloat(localStorage.getItem('revaro_lat'));
    const cachedLng = parseFloat(localStorage.getItem('revaro_lng'));
    if (!isNaN(cachedLat) && !isNaN(cachedLng)) {
        useLocation(cachedLat, cachedLng);
    }
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function (position) {
            localStorage.setItem('revaro_lat', position.coords.latitude);
            localStorage.setItem('revaro_lng', position.coords.longitude);
            useLocation(position.coords.latitude, position.coords.longitude);
        }, locationUnavailable);
    } else {
        locationUnavailable();
    }

    function locationUnavailable() {
        if (userLocation) {
            return;
        }
        document.getElementById('locationOffMsg').hidden = false;
        document.getElementById('distanceFilter').classList.add('disabled');
    }

    function useLocation(lat, lng) {
        userLocation = { lat: lat, lng: lng };
        cards.forEach(function (card) {
            const distance = distanceTo(card);
            const label = card.querySelector('.event-distance');
            if (label && distance !== null) {
                label.textContent = distance < 1 ? '< 1 mi' : Math.round(distance) + ' mi';
            }
        });
        sortFeed();
    }

    function distanceTo(card) {
        const lat = parseFloat(card.dataset.lat);
        const lng = parseFloat(card.dataset.lng);
        if (!userLocation || isNaN(lat) || isNaN(lng)) {
            return null;
        }
        return haversineMiles(userLocation.lat, userLocation.lng, lat, lng);
    }

    function haversineMiles(lat1, lng1, lat2, lng2) {
        const earthRadiusMiles = 3958.8;
        const toRadians = function (degrees) { return degrees * Math.PI / 180; };
        const dLat = toRadians(lat2 - lat1);
        const dLng = toRadians(lng2 - lng1);
        const a = Math.sin(dLat / 2) ** 2
            + Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.sin(dLng / 2) ** 2;
        return earthRadiusMiles * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // Lower is better. Soon beats far away, and anything within 50 miles isn't penalized for distance.
    function relevanceScore(card, now) {
        const daysUntil = (new Date(card.dataset.date).getTime() - now) / 86400000;
        if (isNaN(daysUntil) || daysUntil < 0) {
            return PAST_EVENT_SCORE;
        }
        const miles = distanceTo(card) ?? NO_LOCATION_MILES;
        const distancePenalty = miles < 50 ? 1 : Math.pow(miles / 50, 0.2);
        return Math.pow(Math.max(daysUntil, 0.1), 0.8) * distancePenalty;
    }

    function sortFeed() {
        const sort = sortInput.value;
        if (!feed || (sort !== 'relevance' && sort !== 'distance')) {
            return;
        }
        const now = Date.now();
        const score = sort === 'distance'
            ? function (card) { return distanceTo(card) ?? Number.MAX_VALUE; }
            : function (card) { return relevanceScore(card, now); };
        cards.slice()
            .sort(function (a, b) { return score(a) - score(b); })
            .forEach(function (card) { feed.appendChild(card); });
    }
})();
