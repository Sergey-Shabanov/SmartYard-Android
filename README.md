# Smart Yard Android

## История проекта
Это приложение, которое было изначально заказано операторм связи LanTa (г. Тамбов) у студии мобильной разработки MadBrains (г. Ульяновск) в 2020 году для проекта умных домофонов. Это изначально был MVP, который умел принимать видеозвонки с IP домофонов Beward, открывать двери, калитки, шлагбаумы, принимать оплату от клиентов, подтверждать доступ пользователя к адресу, оставлять заявки на подключение, отображать камеры видеонаблюлюдения с архивом, получать и отображать текстовые уведомления, вести чат с оператором, управлять настройками домофона и управлять доступами для других жителей квартиры.
Позже мы стали развивать этот проект совими силами и дополнять его дополнительными фичами. мы добавили: видовые камеры, журнал событий, настройки функции распознавания лиц, а также исправляли баги, которые всплывали за время работы то там, то тут.

В октябре 2021 года мы созрели для того, чтобы открыть исходные коды нашего проекта и предлгаем всем, кто заинтересован в построении аналогичных сервисов не "изобретать свой велосипед" с нуля, а вместе с нами развивать данный проект, обмениваясь идеями и наработками. На этот момент приложением пользуется около 15 тысяч пользователей, живущих в домах оборудованных домофонными панелями и системами видеонаблюдения от нашей компании.

## API
Приложение использует наше собственное [API](https://rosteleset.github.io/ApplicationAPI/).
Код back-end, реализующий это API на текущий момент неотделим от нашей архитектуры и наших всех остальных систем, поэтому на текущем этапе мы не можем вам предложить ничего лучше, как реализовать у себя этот API своими силами.

## Используемые фреймворки и компоненты (основные)
* Liblinphone SDK для реализации SIP части
* Flussonic для работы с архивом видеокамер 
* Firebase Cloud Messaging для работы с Push-уведомлениями
* osmdroid для работы с картами
* Crashlytics, Yandex AppMetrika для сбора информации и сбоях и аналитике
