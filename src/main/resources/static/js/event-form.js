// Event form: date and time picker, recurring date calendar, tag picker, organizer toggle
(function () {
    'use strict';

    const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July',
        'August', 'September', 'October', 'November', 'December'];

    setUpOrganizerToggle();
    setUpDateTimePicker();
    setUpRecurringCalendar();
    setUpTagPicker();

    function setUpOrganizerToggle() {
        const nameField = document.getElementById('organizerNameField');
        document.querySelectorAll('input[name="postedByOrganizer"]').forEach(function (radio) {
            radio.addEventListener('change', function () {
                nameField.hidden = radio.value === 'true';
            });
        });
    }

    // The form posts a single yyyy-MM-ddTHH:mm value built from a date input and a time dropdown
    function setUpDateTimePicker() {
        const hidden = document.getElementById('eventDateTime');
        const datePicker = document.getElementById('datePicker');
        const timePicker = document.getElementById('timePicker');

        for (let hour = 0; hour < 24; hour++) {
            for (let minute = 0; minute < 60; minute += 15) {
                const label = (hour % 12 === 0 ? 12 : hour % 12) + ':' + pad(minute) + (hour < 12 ? ' AM' : ' PM');
                timePicker.add(new Option(label, pad(hour) + ':' + pad(minute)));
            }
        }

        // When editing, split the saved value back into both inputs, rounded to the nearest 15 minutes
        const saved = hidden.value.match(/^(\d{4}-\d{2}-\d{2})T(\d{2}):(\d{2})/);
        if (saved) {
            let hour = parseInt(saved[2], 10);
            let minute = Math.round(parseInt(saved[3], 10) / 15) * 15;
            if (minute === 60) {
                minute = 0;
                hour = (hour + 1) % 24;
            }
            datePicker.value = saved[1];
            timePicker.value = pad(hour) + ':' + pad(minute);
            update();
        }

        datePicker.addEventListener('change', update);
        timePicker.addEventListener('change', update);

        function update() {
            hidden.value = datePicker.value && timePicker.value ? datePicker.value + 'T' + timePicker.value : '';
        }
    }

    // Each picked day becomes its own event at the same time as the main one
    function setUpRecurringCalendar() {
        const checkbox = document.getElementById('recurringCheck');
        const panel = document.getElementById('recurringOptions');
        const grid = document.getElementById('calendarGrid');
        const monthLabel = document.getElementById('calendarMonth');
        const inputs = document.getElementById('specificDateInputs');
        const summary = document.getElementById('selectedDatesSummary');
        const selected = new Set();
        const today = new Date();
        const startOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate());
        let year = today.getFullYear();
        let month = today.getMonth();

        checkbox.addEventListener('change', function () {
            panel.hidden = !checkbox.checked;
            render();
            updateInputs();
        });
        document.getElementById('prevMonth').addEventListener('click', function () { changeMonth(-1); });
        document.getElementById('nextMonth').addEventListener('click', function () { changeMonth(1); });

        function changeMonth(delta) {
            month += delta;
            if (month < 0) {
                month = 11;
                year--;
            } else if (month > 11) {
                month = 0;
                year++;
            }
            render();
        }

        function render() {
            monthLabel.textContent = MONTHS[month] + ' ' + year;
            grid.replaceChildren();
            const firstWeekday = new Date(year, month, 1).getDay();
            const daysInMonth = new Date(year, month + 1, 0).getDate();

            for (let i = 0; i < firstWeekday; i++) {
                grid.appendChild(document.createElement('div'));
            }
            for (let day = 1; day <= daysInMonth; day++) {
                const date = year + '-' + pad(month + 1) + '-' + pad(day);
                const cell = document.createElement('button');
                cell.type = 'button';
                cell.className = 'calendar-day';
                cell.textContent = day;
                cell.disabled = new Date(year, month, day) < startOfToday;
                cell.classList.toggle('selected', selected.has(date));
                cell.addEventListener('click', function () {
                    if (selected.has(date)) {
                        selected.delete(date);
                    } else {
                        selected.add(date);
                    }
                    cell.classList.toggle('selected', selected.has(date));
                    updateInputs();
                });
                grid.appendChild(cell);
            }
        }

        // Unchecking the box keeps the picks on screen but stops them from being submitted
        function updateInputs() {
            const dates = Array.from(selected).sort();
            const submitted = checkbox.checked ? dates : [];
            inputs.replaceChildren(...submitted.map(function (date) {
                return hiddenInput('specificDates', date);
            }));

            if (dates.length === 0) {
                summary.textContent = 'No dates selected yet.';
                return;
            }
            const count = document.createElement('strong');
            count.textContent = dates.length + (dates.length === 1 ? ' date' : ' dates') + ' selected: ';
            summary.replaceChildren(count, dates.map(formatDate).join(', '));
        }

        function formatDate(date) {
            const parts = date.split('-');
            return MONTHS[parseInt(parts[1], 10) - 1] + ' ' + parseInt(parts[2], 10);
        }
    }

    // The checkboxes in the modal are the source of truth. Closing the modal copies them
    // into hidden inputs and the preview.
    function setUpTagPicker() {
        const modal = document.getElementById('tagPickerModal');
        const checkboxes = Array.from(modal.querySelectorAll('input[type="checkbox"]'));
        const inputs = document.getElementById('tagInputs');
        const preview = document.getElementById('selectedTags');
        const count = document.getElementById('selectedTagCount');

        checkboxes.forEach(function (checkbox) {
            checkbox.addEventListener('change', updateCount);
        });
        modal.addEventListener('hidden.bs.modal', applySelection);
        updateCount();
        applySelection();

        function chosen() {
            return checkboxes.filter(function (checkbox) { return checkbox.checked; });
        }

        function updateCount() {
            const total = chosen().length;
            count.textContent = total + (total === 1 ? ' tag' : ' tags') + ' selected';
        }

        function applySelection() {
            const tags = chosen();
            inputs.replaceChildren(...tags.map(function (checkbox) {
                return hiddenInput('tagIds', checkbox.value);
            }));
            if (tags.length === 0) {
                const empty = document.createElement('span');
                empty.className = 'form-hint';
                empty.textContent = 'No tags yet. Click "Add Tags" to pick some.';
                preview.replaceChildren(empty);
                return;
            }
            preview.replaceChildren(...tags.map(function (checkbox) {
                const pill = document.createElement('span');
                pill.className = 'tag-pill';
                pill.textContent = checkbox.dataset.name;
                pill.style.setProperty('--tag-bg', checkbox.dataset.bg);
                pill.style.setProperty('--tag-color', checkbox.dataset.color);
                return pill;
            }));
        }
    }

    function hiddenInput(name, value) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value;
        return input;
    }

    function pad(number) {
        return String(number).padStart(2, '0');
    }
})();
