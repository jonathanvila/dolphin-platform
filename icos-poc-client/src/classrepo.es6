"use strict";
import {exists} from './helpers.es6'

const UNKNOWN = 0, BASIC_TYPE = 1, ENUM = 2, DOLPHIN_BEAN = 3;

export class ClassRepository {

    constructor() {
        this.classes = new Map();
        this.beans = new Map();
        this.enums = new Map();
        this.classInfos = new Map();
    }

    setValueType(property, value) {
        if (!exists(value)) {
            return;
        }
        switch (value) {
            case UNKNOWN:
                if (!exists(property.get)) {
                    property.get = () => null;
                }
                break;
            case BASIC_TYPE:
                property.get = (value) => value;
                break;
            case ENUM:
                property.get = (ord) => {
                    let enumInfo = this.enums.get(property.type);
                    return exists(enumInfo) ? enumInfo[ord] : null;
                }
                break;
            case DOLPHIN_BEAN:
                property.get = (id) => this.beans.get(id);
                break;
        }
    }

    static setValue(property, value) {
        if (exists(value)) {
            property.type = value;
        }
    }

    registerClass(model) {
        if (this.classes.has(model.id)) {
            return;
        }
        console.debug("ClassRepository.registerClass", model);

        let classInfo = {};
        for (let attribute of model.attributes) {
            let property = classInfo[attribute.propertyName];
            if (!exists(property)) {
                property = classInfo[attribute.propertyName] = {};
            }

            switch (attribute.tag) {
                case opendolphin.Tag.valueType():
                    attribute.onValueChange(event =>
                        this.setValueType(property, event.newValue)
                    );
                    break;
                case opendolphin.Tag.value():
                    attribute.onValueChange(event =>
                        ClassRepository.setValue(property, event.newValue)
                    );
                    break;
            }
        }
        this.classes.set(model.id, classInfo);
    }

    registerEnum(model) {
        if (this.enums.has(model.id)) {
            return;
        }
        console.debug("ClassRepository.registerEnum", model);

        let enumInfo = []
        for (let attribute of model.attributes) {
            enumInfo[parseInt(attribute.propertyName)] = attribute.value;
        }
        this.enums.set(model.id, enumInfo);
    }

    unregisterClass(model) {
        console.debug("ClassRepository.unregisterClass", model);
        this.classes.delete(model.id);
    }

    unregisterEnum(model) {
        console.debug("ClassRepository.unregisterEnum", model);
        this.enums.delete(model.id);
    }

    load(model) {
        console.debug("ClassRepository.load():", model);
        let classInfo = this.classes.get(model.presentationModelType);
        let bean = {};
        for (let attribute of model.attributes) {
            let property = attribute.propertyName;
            attribute.onValueChange(event =>
                bean[property] = classInfo[property].get(event.newValue)
            );
        }
        this.beans.set(model.id, bean);
        this.classInfos.set(model.id, classInfo);
        return bean;
    }

    unload(model) {
        console.debug("ClassRepository.unload():", model);
        let bean = this.beans.get(model.id);
        if (exists(bean)) {
            this.beans.delete(model.id);
        }
        this.classInfos.delete(model.id);
        return bean;
    }

    modifyList(source, attribute, from, count, newElements) {
        let bean = this.beans.get(source);
        if (exists(bean)) {
            let list = bean[attribute];
            if (!exists(list)) {
                bean[attribute] = list = [];
            } else if (!Array.isArray(list)) {
                bean[attribute] = list = [list];
            }
            list.splice(from, count, newElements);
        }
    }

    addListEntry(model) {
        console.debug("ClassRepository.addListEntry", model);
        let source    = model.findAttributeByPropertyName('source').value;
        let attribute = model.findAttributeByPropertyName('attribute').value;
        let pos       = model.findAttributeByPropertyName('pos').value;
        let element   = model.findAttributeByPropertyName('element').value;
        let classInfo = this.classInfos.get(source);
        if (exists(classInfo)) {
            let entry = classInfo[attribute].get(element);
            if (exists(entry)) {
                this.modifyList(source, attribute, pos, 0, entry);
            }
        }
    }

    delListEntry(model) {
        console.debug("ClassRepository.delListEntry", model);
        let source    = model.findAttributeByPropertyName('source').value;
        let attribute = model.findAttributeByPropertyName('attribute').value;
        let from      = model.findAttributeByPropertyName('from').value;
        let to        = model.findAttributeByPropertyName('to').value;
        this.modifyList(source, attribute, from, to-from);

    }

    setListEntry(model) {
        console.debug("ClassRepository.setListEntry", model);
        let source    = model.findAttributeByPropertyName('source').value;
        let attribute = model.findAttributeByPropertyName('attribute').value;
        let pos       = model.findAttributeByPropertyName('pos').value;
        let element   = model.findAttributeByPropertyName('element').value;
        let classInfo = this.classInfos.get(source);
        if (exists(classInfo)) {
            let entry = classInfo[attribute].get(element);
            if (exists(entry)) {
                this.modifyList(source, attribute, pos, 1, entry);
            }
        }
    }
}
