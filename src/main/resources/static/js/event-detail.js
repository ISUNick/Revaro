// Event page: share button, @mention autocomplete, replies, and mention hover cards
(function () {
    'use strict';

    const MENTION_PATTERN = /@([A-Za-z0-9_]{3,30})/g;

    setUpShareButton();
    setUpCommentBox();
    linkifyMentions();
    setUpMentionCards();

    function setUpShareButton() {
        const button = document.getElementById('shareButton');
        button.addEventListener('click', function () {
            const url = window.location.href;
            if (navigator.share) {
                navigator.share({ title: document.title, url: url }).catch(function () {});
                return;
            }
            navigator.clipboard.writeText(url).then(function () {
                const original = button.innerHTML;
                button.innerHTML = '<i class="bi bi-check me-1"></i>Copied!';
                setTimeout(function () { button.innerHTML = original; }, 2000);
            }).catch(function () {
                window.prompt('Copy this link:', url);
            });
        });
    }

    function setUpCommentBox() {
        const input = document.getElementById('commentInput');
        const dropdown = document.getElementById('mentionDropdown');
        if (!input) {
            return;
        }
        let mentionStart = -1;
        let latestQuery = '';

        input.addEventListener('input', function () {
            const beforeCursor = input.value.substring(0, input.selectionStart);
            const match = beforeCursor.match(/@([A-Za-z0-9_]*)$/);
            if (!match || match[1].length === 0) {
                dropdown.hidden = true;
                return;
            }
            mentionStart = beforeCursor.length - match[0].length;
            fetchSuggestions(match[1]);
        });

        input.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                dropdown.hidden = true;
            }
        });

        document.addEventListener('click', function (e) {
            if (e.target !== input) {
                dropdown.hidden = true;
            }
        });

        document.querySelectorAll('[data-reply]').forEach(function (button) {
            button.addEventListener('click', function () {
                input.value = '@' + button.dataset.reply + ' ';
                input.focus();
                input.scrollIntoView({ behavior: 'smooth', block: 'center' });
                input.setSelectionRange(input.value.length, input.value.length);
            });
        });

        function fetchSuggestions(query) {
            latestQuery = query;
            fetch('/api/users/search?q=' + encodeURIComponent(query))
                .then(function (response) { return response.ok ? response.json() : []; })
                .then(function (users) {
                    // Ignore slow responses for something the user already typed past
                    if (query === latestQuery) {
                        showSuggestions(users);
                    }
                })
                .catch(function () { dropdown.hidden = true; });
        }

        function showSuggestions(users) {
            dropdown.replaceChildren(...users.map(function (user) {
                const option = document.createElement('div');
                option.className = 'mention-option';
                option.append(avatarElement(user, 'avatar-xs'), textElement('span', '', '@' + user.username));
                // mousedown instead of click so the textarea keeps focus
                option.addEventListener('mousedown', function (e) {
                    e.preventDefault();
                    insertMention(user.username);
                });
                return option;
            }));
            dropdown.hidden = users.length === 0;
        }

        function insertMention(username) {
            const before = input.value.substring(0, mentionStart);
            const after = input.value.substring(input.selectionStart);
            input.value = before + '@' + username + ' ' + after;
            const cursor = before.length + username.length + 2;
            input.focus();
            input.setSelectionRange(cursor, cursor);
            dropdown.hidden = true;
        }
    }

    // Built with DOM nodes so comment text is never parsed as HTML
    function linkifyMentions() {
        document.querySelectorAll('.comment-body').forEach(function (body) {
            const text = body.textContent;
            if (!text.includes('@')) {
                return;
            }
            const parts = [];
            let last = 0;
            for (const match of text.matchAll(MENTION_PATTERN)) {
                parts.push(text.substring(last, match.index));
                const link = document.createElement('a');
                link.className = 'mention-tag';
                link.href = '/profile/' + match[1];
                link.dataset.username = match[1];
                link.textContent = match[0];
                parts.push(link);
                last = match.index + match[0].length;
            }
            parts.push(text.substring(last));
            body.replaceChildren(...parts);
        });
    }

    function setUpMentionCards() {
        const card = document.createElement('div');
        card.className = 'mention-card';
        card.hidden = true;
        document.body.appendChild(card);
        let hideTimer = null;
        let currentUsername = null;

        document.addEventListener('mouseover', function (e) {
            const tag = e.target.closest('.mention-tag');
            if (tag) {
                show(tag);
            }
        });
        document.addEventListener('mouseout', function (e) {
            const tag = e.target.closest('.mention-tag');
            if (tag && !card.contains(e.relatedTarget)) {
                hideSoon();
            }
        });
        card.addEventListener('mouseenter', function () { clearTimeout(hideTimer); });
        card.addEventListener('mouseleave', hideSoon);

        function show(tag) {
            clearTimeout(hideTimer);
            const username = tag.dataset.username;
            if (username === currentUsername && !card.hidden) {
                return;
            }
            currentUsername = username;
            position(tag);
            card.replaceChildren(textElement('div', 'mention-card-meta', 'Loading...'));
            card.hidden = false;

            fetch('/api/users/' + encodeURIComponent(username) + '/preview')
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error('No profile for ' + username);
                    }
                    return response.json();
                })
                .then(function (user) {
                    if (username === currentUsername) {
                        fill(user);
                    }
                })
                .catch(function () {
                    card.replaceChildren(profileLink(username, '@' + username));
                });
        }

        function hideSoon() {
            hideTimer = setTimeout(function () {
                card.hidden = true;
                currentUsername = null;
            }, 150);
        }

        // Below the mention, or above it if there isn't room
        function position(tag) {
            const rect = tag.getBoundingClientRect();
            const top = rect.bottom + 168 > window.innerHeight ? rect.top - 168 : rect.bottom + 8;
            card.style.top = top + 'px';
            card.style.left = Math.min(rect.left, window.innerWidth - 230) + 'px';
        }

        function fill(user) {
            const details = document.createElement('div');
            details.append(textElement('div', 'mention-card-name', '@' + user.username),
                    textElement('div', 'mention-card-meta', user.revPoints + ' Rev Points'));
            const header = document.createElement('div');
            header.className = 'mention-card-header';
            header.append(avatarElement(user, 'avatar-md'), details);
            card.replaceChildren(header,
                    textElement('div', 'mention-card-meta mb-2', 'Joined ' + user.joined),
                    profileLink(user.username, 'View Profile'));
        }
    }

    function profileLink(username, text) {
        const link = textElement('a', 'mention-card-link', text);
        link.href = '/profile/' + encodeURIComponent(username);
        return link;
    }

    // Same markup as the avatar fragment in layout.html
    function avatarElement(user, sizeClass) {
        const avatar = document.createElement('div');
        avatar.className = 'avatar ' + sizeClass;
        if (user.avatar) {
            const img = document.createElement('img');
            img.src = user.avatar;
            img.alt = user.username;
            avatar.appendChild(img);
        } else {
            avatar.textContent = user.username.charAt(0).toUpperCase();
        }
        return avatar;
    }

    function textElement(tag, className, text) {
        const element = document.createElement(tag);
        element.className = className;
        element.textContent = text;
        return element;
    }
})();
