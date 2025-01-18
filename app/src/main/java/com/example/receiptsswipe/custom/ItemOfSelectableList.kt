package com.example.receiptsswipe.custom

interface ItemOfSelectableList : RecyclerElement {

    /**
     * id возвращает String - идентификатор элемента выбора
     */
    val id: String

    /**
     * title возвращает String - наименование элемента в списке
     */
    val title: String

    /**
     * isSelected возвращает Boolean - признак выбранного элемента
     */
    val isSelected: Boolean

    /**
     * isDisable возвращает Boolean -  Ориентируемся на этот флаг, в случае если получаем item, который у нас уже имеется в кеше.
     */
    val isDisable: Boolean

    /**
     * chevronVisibility возвращает Boolean - признак выбранного элемента, если выбрано 2 и более элементов
     */
    val chevronVisibility: Boolean

    /**
     * itemIsSame возвращает Boolean true - если это один и тот же элемент, false - в противном случае.
     * Обычно достаточно дефолтной имплементации this.id == otherItem.id. Это сравнение используется
     * для подмены элемента с отличным признаком isSelected.
     *
     * @param otherItem тип ItemOfSelectableList - другой элемент к сравнению
     * @return Boolean
     */
    fun itemIsSame(otherItem: ItemOfSelectableList?): Boolean = this.id == otherItem?.id

    /**
     * copyDiff возвращает копию объекта с отличным значением свойства, что указано в параметре.
     * Для имплементации используется стандартный метод copy в дата классе.
     *
     * @param isSelected - новое значение свойства isSelected
     * @return ItemOfSelectableList - новый объект
     */
    fun copyDiff(
        isDisable: Boolean = false,
        isSelected: Boolean = false,
        chevronVisibility: Boolean = false
    ): ItemOfSelectableList

    /**
     * encodeToStringJson - возвращает строку json. Для сериализации должен использоваться Kotlin serialization,
     * так как это используется при обратной десериализации объекта при возврате результата выбора.
     *
     * @return String - json представления объекта.
     */
    fun encodeToStringJson(): String
}