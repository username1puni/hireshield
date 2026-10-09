INSERT INTO rule_config (code, title, description, points) VALUES
('FEE_REQUEST',      'Asks you to pay money',            'Registration, training, security deposit, laptop or kit fee before joining', 30),
('TASK_SCAM',        'Task-based earning scheme',        'Earn by liking videos, rating hotels or maps, prepaid tasks, daily commission', 25),
('PERSONAL_DATA',    'Asks for sensitive details',       'OTP, bank details, Aadhaar or PAN asked for before any interview', 20),
('LOOKALIKE_DOMAIN', 'Look-alike company website',       'Domain is very close to a well known brand but not the real one', 20),
('FAKE_GOVT',        'Fake government job',              'Claims a government job but the site is not on .gov.in or .nic.in', 20),
('CHAT_ONLY',        'Contact only on WhatsApp/Telegram','No official channel, only chat apps', 15),
('FREE_EMAIL',       'Recruiter uses free email',        'A company recruiter writing from gmail, yahoo, outlook and similar', 15),
('HIGH_SALARY',      'Pay too good to be true',          'Very high pay for simple work, for example Rs 5,000 a day for data entry', 15),
('NEW_DOMAIN',       'Very new website',                 'Company domain registered less than 6 months ago', 15),
('NO_INTERVIEW',     'Selected without interview',       'Direct selection or offer without any interview', 10),
('URGENCY',          'Pressure to act fast',             'Limited seats, join today, reply within 24 hours', 10),
('POOR_WRITING',     'Unprofessional writing',           'Lots of capital letters, emojis or exclamation marks', 5);
